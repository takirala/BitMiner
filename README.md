# README #

How to run ? 

After navigating to the folder containing Project1.scala and build.sbt,
* Enter sbt interactive shell mode and do a compile. Then :

    * for Master mode : > run 3 
    * for Client mode : > run 127.0.0.1

where 3 is the number of leading zeroes for the resulting bit coin and 127.0.0.1 is the server ip address.


### What is this repository for? ###

Bitcoins (see http://en.wikipedia.org/wiki/Bitcoin) are the most popular crypto-currency in common use. At their hart, bitcoins use the hardness of cryp-tographic hashing (for a reference see http://en.wikipedia.org/wiki/Cryptographic hash function) to ensure a limited supply of coins. In particular, the key component in a bitcoin is an input that, when hashed produces an output smaller than a target value. In practice, the comparison values have leading 0's, thus the bitcoin is required to have a given number of leading 0's (to ensure 3 leading 0's, you look for hashes smaller than 0x001000... or smaller or equal to 0x000ff.... The hash you are required to use is SHA-256. The goal of this project is to use Scala and the actor model to build a good solution to this problem that runs well on multi-core machines.
