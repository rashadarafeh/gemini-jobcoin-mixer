# gemini-jobcoin-mixer

## Usage:
 To run:
 `sbt "run"`
 
You can now use the endpoint to create destination addresses for yourself, you will be returned a new deposit address:
`http://localhost:9000/createAccounts/address1,address2`

Now you can transfer jobcoin into the returned deposit account address, and those funds will be distributed into the addresses provided for that deposit account.

## Discussion:

So I liked the idea of writing the JobcoinMixer as a microservice, with an endpoint to create/submit distribution addresses. The endpoint returns a new deposit address to be used as the source to distribute funds to the submitted addresses.

Endpoint format: `http://localhost:9000/createAccounts/address1,address2`

Returns: `{"depositAddress": "whatever-generated-name"}`

The other part of this is polling for whether there are funds to distribute. I actually implemented 2 approaches:

Solution 1: Can be used by uncommenting the line calling `backend.startPollingBalances()` in JobCoinMixerComponents. Distributes funds from the created deposit accounts by polling the balances of those particular accounts. If there is a balance in the account, distribute it to the destination addresses. 

Advantage: The transaction history endpoint will return an ever growing list of transactions, many we don't care about, most we have already processed. Don't need to bother with that, we only look at balances for the accounts we care about!

Disadvantage: Not really scalable to production level/amounts of data/accounts. If the number of deposit accounts grow, this becomes an inefficient way to do it.

Solution 2: Can be used by uncommenting the line calling `backend.startPollingTransactions()` in JobCoinMixerComponents. Distributes funds from the created accounts by polling the transaction history, checking for "new messages" targeting the deposit accounts we created, and distributes the transfer amount to the destination addresses.

Advantage: Process the most recent messages only. Don't need to check balances of all our deposit accounts to move funds

Disadvantage: The transaction history api will return an ever growing list of transactions, many we don't care about, most we have already processed. we will be checking the entire output for new messages each time

#### In a production setting, it coud be most ideal that transactions/events would be consumed off a queue and processed on arrival



