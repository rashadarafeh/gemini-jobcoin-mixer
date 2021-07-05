# gemini-jobcoin-mixer

## Usage:
 To run:
 `sbt "runMain com.gemini.jobcoin.JobcoinMixer"`
 
You can now use the endpoint to create desitnation addresses for yourself, you will be returned the new deposit address:
`http://localhost:5432/api/createAdresses/address1,address2,address3`

Now you can transfer jobcoin into the returned deposit account address, and those funds will be distributed into the adresses provided for that deposit account.

## Discussion:

So I liked the idea of writing the JobcoinMixer as a microservice, with an endpoint to create/submit distribution addresses. The endpoint returns a new desposit address to be used as the source to distribute funds to the submitted addresses.

Endpoint format: `http://locahost:5432/api/createAddresses/name1,name2,name3`

Returns: `{"depositAddress": "whatever-generated-name"}`

The other part of this is polling for transactions to the deposit accounts we created. I actually implemented 2 approaches. One which I thought might simplify the problem at first, but ended up doing the other method, as it didnt really simplify it!

Solution 1: Distributes funds from the created deposit accounts by polling the balances of those particular created deposit accounts. If there is a balance in the account, distribute it to the desitnation addresses. 

Advantage: the transaction history api will return an ever growing list of transactions, many we dont care about, most we have already processed. Dont need to bother with that, we only look at balances for hte accounts we care about!

Disadvantage: Not really scalable to production level/amounts of data. if the number of deposit accounts grow, this becomes an inefficient way to do it.

Solution 2: Distributes funds from the created accounts by polling the transaction history, checking for "new messages" targeting the deposit accounts we created, and distributes the transer amount to the destination addresses.

Advantage: Process the most recent messages only. dont need to check balances of all our deposit accounts to move funds
Disadvantage: The transaction history api will return an ever growing list of transactions, many we dont care about, most we have already processed. we will be checking the entire output for new messsages each time

#### In a production setting, I'd imagine new transactions/events would be consumed off a queue and processed on arrival



