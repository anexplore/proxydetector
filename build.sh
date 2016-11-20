#!/bin/bash
mvn post-clean
mvn assembly:assembly -Dmaven.test.skip=true
