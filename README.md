# PWRJ

PWRJ is a Java library for interacting with the PWR network. It provides an easy interface for wallet management and sending transactions on PWR.

## Features

- Generate wallets and manage keys 
- Get wallet balance and nonce
- Build, sign and broadcast transactions
- Transfer PWR tokens
- Send data to PWR virtual machines
- Interact with PWR nodes via RPC

## Getting Started

### Prerequisites

- Java 8+

### Installation

PWRJ is available on Maven Central. Add this dependency to your `pom.xml`:

```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.github.pwrlabs</groupId>
            <artifactId>pwrj</artifactId>
            <version>1.0.6</version>
        </dependency>
    </dependencies>
```

Or Gradle:

Add it in your root build.gradle at the end of repositories:

	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
		implementation 'com.github.pwrlabs:pwrj:1.0.6'
	}

### Usage

**Import the library:**
```java 
import com.github.pwrlabs.pwrj.*;
```

**Set your RPC node:**
```java
PWRJ.setRpcNodeUrl("https://pwrrpc.pwrlabs.io/");
```

**Generate a new wallet:** 
```java
PWRWallet wallet = new PWRWallet(); 
```

You also have the flexibility to import existing wallets using a variety of constructors
```java
String privateKey = "private key"; //Replace with hex private key
PWRWallet wallet = new PWRWallet(privateKey); 
```
```java
byte[] privateKey = ...; 
PWRWallet wallet = new PWRWallet(privateKey); 
```
```java
ECKeyPair ecKeyPair = ...; //Generate or import ecKeyPair 
PWRWallet wallet = new PWRWallet(ecKeyPair); 
```

**Get wallet address:**
```java
String address = wallet.getAddress();
```

**Get wallet balance:**
```java
long balance = wallet.getBalance();
```

**Get private key:**
```java
BigInteger privateKey = wallet.getPrivateKey();
```

**Transfer PWR tokens:**
```java
wallet.transferPWR("recipientAddress", 1000); 
```

Sending a transcation to the PWR Chain returns a Response object, which specified if the transaction was a success, and returns relevant data.
If the transaction was a success, you can retrieive the transaction hash, if it failed, you can fetch the error.

```java
Response r = wallet.transferPWR("recipientAddress", 1000); 

if(r.isSuccess()) {
   System.out.println("Transcation Hash: " + r.getMessage());
} else {
   System.out.println("Error: " + r.getError());
}
```

**Send data to a VM:**
```java
int vmId = 123;
byte[] data = ...;
Response r = wallet.sendVmDataTxn(vmId, data);

if(r.isSuccess()) {
   System.out.println("Transcation Hash: " + r.getMessage());
} else {
   System.out.println("Error: " + r.getError());
}
```
### Other Static Calls

**Update fee per byte:**

Fetches latest fee-per-byte rate from the RPC node and updates the local fee rate.

```java
PWRJ.updateFeePerByte();
``` 

**Get RPC Node Url:**

Returns currently set RPC node URL.

```java
String url = PWRJ.getRpcNodeUrl();
```

**Get Fee Per Byte: **

Gets the latest fee-per-byte rate.

```java
long fee = PWRJ.getFeePerByte();
```

**Get Balance Of Address:**

Gets the balance of a specific address.

```java
long balance = PWRJ.getBalanceOfAddress("0x...");
```

**Get Nonce Of Address:**

Gets the nonce/transaction count of a specific address.

```java
int nonce = PWRJ.getNonceOfAddress("0x..."); 
```

**Broadcast Txn:**

Broadcasts a signed transaction to the network.

```java
byte[] signedTransaction = ...;
PWRJ.broadcastTxn(signedTransaction);
```

## Contributing

Pull requests are welcome! 

For major changes, please open an issue first to discuss what you would like to change.

## License

[MIT](https://choosealicense.com/licenses/mit/)
