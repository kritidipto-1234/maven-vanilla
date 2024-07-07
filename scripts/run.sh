#!/bin/bash
mvn compile 
mvn exec:java -Dexec.mainClass="com.example.VanillaHttpServer"