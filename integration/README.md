## Welcome to ORION local testing

### Initialize test environment

1. Open `<this-folder>/environment/init-setup.sh` file and set your workspace folder by changing
   the `WORKSPACE` variable. The default is set to `~/Desktop/temp/orion-workspace
   ` folder. Also, make sure the path variables to master and worker jar files configured correctly.
2. Execute the init script with `sh init-setup.sh`
3. Open postman and execute the test suit by importing the collection
   `<this-folder>/test-scripts/ORION Test Suit.postman_collection.json`. The same folder includes
   all the task files needed.
4. Once tests completed, execute `sh stop-setup.sh` script to stop the services. Otherwise, the
   services may not be killed properly and be left in the OS.

Happy testing!


