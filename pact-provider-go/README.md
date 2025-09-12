
### Pre-requisites

Follow the installation instructions of Pact-Go here: https://docs.pact.io/implementation_guides/go/readme#installation 

Find below the steps I followed:
1. Install Go via Homebrew: `brew install go`.
2. Add the following in the bash profile `.zshrc`:
    ```bash
   export GOPATH=$HOME/go
   export PATH=$PATH:$GOPATH/bin
   ```
3. Install library in project: `go get github.com/pact-foundation/pact-go/v2`.
4. Install Pact Go CLI in OS: `go install github.com/pact-foundation/pact-go/v2`.
5. Execute Pact Go CLI installation to setup Pact in the OS: `pact-go install`.
6. Now when executing `go test` the Pact Provider's tests have all necessary libraries to be executed. 
