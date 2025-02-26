// SPDX-License-Identifier: MIT
pragma solidity ^0.8.17;

/**
 * @title TimelockMultisigWallet
 * @dev A multisignature wallet with a timelock feature for added security.
 * Supports both ETH and ERC20 token transfers with a 48-hour cooldown period.
 */
interface IERC20 {
    function transfer(address to, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
    function transferFrom(address sender, address recipient, uint256 amount) external returns (bool);
}

contract TimelockMultisigWallet {
    // Events
    event OwnerAdded(address indexed owner);
    event ThresholdChanged(uint256 newThreshold);
    event TransactionCreated(uint256 indexed txId, address indexed to, uint256 value, bytes data, address tokenAddress);
    event TransactionApprovalVote(uint256 indexed txId, address indexed owner);
    event TransactionApproved(uint256 indexed txId);
    event TransactionCancelled(uint256 indexed txId);
    event TransactionCancellationVote(uint256 indexed txId, address indexed owner);
    event TransactionExecuted(uint256 indexed txId);
    event Deposit(address indexed sender, uint256 amount);

    // Transaction structure
    struct Transaction {
        address to;
        uint256 value;
        bytes data;
        bool executed;
        bool cancelled;
        uint256 approvalCount;
        uint256 cancellationCount;
        uint256 timestamp;
        address tokenAddress; // address(0) for ETH, otherwise the ERC20 token address
        uint256 amount;      // Amount of token to send
    }

    // Constants
    uint256 public constant TIMELOCK_DURATION = 48 hours;

    // State variables
    address[] public owners;
    mapping(address => bool) public isOwner;
    uint256 public threshold;
    Transaction[] public transactions;
    mapping(uint256 => mapping(address => bool)) public approved;
    mapping(uint256 => mapping(address => bool)) public cancelRequested;

    // Modifiers
    modifier onlyOwner() {
        require(isOwner[msg.sender], "Not an owner");
        _;
    }

    modifier txExists(uint256 _txId) {
        require(_txId < transactions.length, "Transaction does not exist");
        _;
    }

    modifier notExecuted(uint256 _txId) {
        require(!transactions[_txId].executed, "Transaction already executed");
        _;
    }

    modifier notCancelled(uint256 _txId) {
        require(!transactions[_txId].cancelled, "Transaction already cancelled");
        _;
    }

    modifier canExecute(uint256 _txId) {
        require(transactions[_txId].approvalCount >= threshold, "Not enough approvals");
        require(block.timestamp >= transactions[_txId].timestamp + TIMELOCK_DURATION, "Timelock period not passed");
        _;
    }

    modifier validThreshold(uint256 _threshold, uint256 _ownerCount) {
        require(_threshold > 0, "Threshold must be greater than 0");
        require(_threshold <= _ownerCount, "Threshold must be less than or equal to owner count");
        _;
    }

    /**
     * @dev Constructor to initialize the multisig wallet
     * @param _owners Array of initial owner addresses
     * @param _threshold Number of required confirmations for a transaction
     */
    constructor(address[] memory _owners, uint256 _threshold) validThreshold(_threshold, _owners.length) {
        require(_owners.length > 0, "Owners required");
        
        for (uint256 i = 0; i < _owners.length; i++) {
            address owner = _owners[i];
            
            require(owner != address(0), "Invalid owner");
            require(!isOwner[owner], "Owner not unique");
            
            isOwner[owner] = true;
            owners.push(owner);
            
            emit OwnerAdded(owner);
        }
        
        threshold = _threshold;
        emit ThresholdChanged(_threshold);
    }

    // Receive function to accept ETH
    receive() external payable {
        emit Deposit(msg.sender, msg.value);
    }

    /**
     * @dev Allows an owner to submit a new ETH transaction
     * @param _to Destination address
     * @param _value Amount of ETH to send
     * @param _data Transaction data payload
     * @return Returns transaction ID
     */
    function submitTransaction(address _to, uint256 _value, bytes memory _data) 
        public 
        onlyOwner 
        returns (uint256) 
    {
        uint256 txId = transactions.length;
        
        transactions.push(Transaction({
            to: _to,
            value: _value,
            data: _data,
            executed: false,
            cancelled: false,
            approvalCount: 0,
            cancellationCount: 0,
            timestamp: 0, // Will be set when approved
            tokenAddress: address(0), // ETH transaction
            amount: _value
        }));
        
        emit TransactionCreated(txId, _to, _value, _data, address(0));
        
        // Auto-approve by submitter
        approveTransaction(txId);
        
        return txId;
    }

    /**
     * @dev Allows an owner to submit a new ERC20 token transaction
     * @param _token Address of the ERC20 token
     * @param _to Recipient address
     * @param _amount Amount of tokens to transfer
     * @return Returns transaction ID
     */
    function submitERC20Transaction(address _token, address _to, uint256 _amount) 
        public 
        onlyOwner 
        returns (uint256) 
    {
        require(_token != address(0), "Invalid token address");
        
        uint256 txId = transactions.length;
        
        // Create the data for the transfer function call
        bytes memory data = abi.encodeWithSignature("transfer(address,uint256)", _to, _amount);
        
        transactions.push(Transaction({
            to: _token,
            value: 0, // No ETH is sent for token transfers
            data: data,
            executed: false,
            cancelled: false,
            approvalCount: 0,
            cancellationCount: 0,
            timestamp: 0, // Will be set when approved
            tokenAddress: _token,
            amount: _amount
        }));
        
        emit TransactionCreated(txId, _to, 0, data, _token);
        
        // Auto-approve by submitter
        approveTransaction(txId);
        
        return txId;
    }

    /**
     * @dev Allows an owner to approve a transaction
     * @param _txId Transaction ID to approve
     */
    function approveTransaction(uint256 _txId) 
        public 
        onlyOwner 
        txExists(_txId) 
        notExecuted(_txId) 
        notCancelled(_txId) 
    {
        require(!approved[_txId][msg.sender], "Transaction already approved");
        
        approved[_txId][msg.sender] = true;
        transactions[_txId].approvalCount += 1;
        
        emit TransactionApprovalVote(_txId, msg.sender);
        
        // Set the timestamp when threshold is reached
        if (transactions[_txId].approvalCount == threshold && transactions[_txId].timestamp == 0) {
            transactions[_txId].timestamp = block.timestamp;
            emit TransactionApproved(_txId);
        }
    }

    /**
     * @dev Allows an owner to request cancellation of an approved but not yet executed transaction
     * @param _txId Transaction ID to cancel
     */
    function requestCancellation(uint256 _txId) 
        public 
        onlyOwner 
        txExists(_txId) 
        notExecuted(_txId) 
        notCancelled(_txId) 
    {
        require(!cancelRequested[_txId][msg.sender], "Cancellation already requested");
        require(transactions[_txId].timestamp > 0, "Transaction not approved yet");
        require(block.timestamp < transactions[_txId].timestamp + TIMELOCK_DURATION, "Timelock period passed");
        
        cancelRequested[_txId][msg.sender] = true;
        transactions[_txId].cancellationCount += 1;
        
        emit TransactionCancellationVote(_txId, msg.sender);
        
        // If cancellation threshold reached, cancel the transaction
        if (transactions[_txId].cancellationCount >= threshold) {
            transactions[_txId].cancelled = true;
            emit TransactionCancelled(_txId);
        }
    }

    /**
     * @dev Allows anyone to execute an approved transaction after the timelock period
     * @param _txId Transaction ID to execute
     */
    function executeTransaction(uint256 _txId) 
        public 
        txExists(_txId) 
        notExecuted(_txId) 
        notCancelled(_txId) 
        canExecute(_txId) 
    {
        Transaction storage transaction = transactions[_txId];
        transaction.executed = true;
        
        if (transaction.tokenAddress == address(0)) {
            // ETH transaction
            (bool success, ) = transaction.to.call{value: transaction.value}(transaction.data);
            require(success, "Transaction execution failed");
        } else {
            // ERC20 transaction
            IERC20 token = IERC20(transaction.tokenAddress);
            bool success = token.transfer(transaction.to, transaction.amount);
            require(success, "ERC20 transfer failed");
        }
        
        emit TransactionExecuted(_txId);
    }

    /**
     * @dev Returns the list of owners
     * @return Array of owner addresses
     */
    function getOwners() public view returns (address[] memory) {
        return owners;
    }

    /**
     * @dev Returns the count of transactions
     * @return Number of transactions
     */
    function getTransactionCount() public view returns (uint256) {
        return transactions.length;
    }

    /**
     * @dev Returns transaction details
     * @param _txId Transaction ID
     * @return to Destination address
     * @return value ETH value
     * @return data Transaction data
     * @return executed Whether the transaction was executed
     * @return cancelled Whether the transaction was cancelled
     * @return approvalCount Number of approvals
     * @return timestamp Time when the transaction was approved
     * @return tokenAddress Address of the token (address(0) for ETH)
     * @return amount Amount of tokens to transfer
     */
    function getTransaction(uint256 _txId) 
        public 
        view 
        txExists(_txId) 
        returns (
            address to,
            uint256 value,
            bytes memory data,
            bool executed,
            bool cancelled,
            uint256 approvalCount,
            uint256 timestamp,
            address tokenAddress,
            uint256 amount
        ) 
    {
        Transaction storage transaction = transactions[_txId];
        
        return (
            transaction.to,
            transaction.value,
            transaction.data,
            transaction.executed,
            transaction.cancelled,
            transaction.approvalCount,
            transaction.timestamp,
            transaction.tokenAddress,
            transaction.amount
        );
    }

    /**
     * @dev Returns the time remaining before a transaction can be executed
     * @param _txId Transaction ID
     * @return Time remaining in seconds, 0 if executable
     */
    function getTimeRemaining(uint256 _txId) 
        public 
        view 
        txExists(_txId) 
        returns (uint256) 
    {
        Transaction storage transaction = transactions[_txId];
        
        if (transaction.executed || transaction.cancelled || transaction.timestamp == 0) {
            return 0;
        }
        
        uint256 endTime = transaction.timestamp + TIMELOCK_DURATION;
        
        if (block.timestamp >= endTime) {
            return 0;
        }
        
        return endTime - block.timestamp;
    }

    /**
     * @dev Checks if a transaction is ready to execute
     * @param _txId Transaction ID
     * @return Whether the transaction can be executed
     */
    function isTransactionReady(uint256 _txId) 
        public 
        view 
        txExists(_txId) 
        returns (bool) 
    {
        Transaction storage transaction = transactions[_txId];
        
        return (
            !transaction.executed &&
            !transaction.cancelled &&
            transaction.approvalCount >= threshold &&
            transaction.timestamp > 0 &&
            block.timestamp >= transaction.timestamp + TIMELOCK_DURATION
        );
    }
}
