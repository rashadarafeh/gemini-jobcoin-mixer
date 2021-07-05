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
Returns {"depositAddress": "whatever-generated-name"}

The other part of this is polling for transactions to the deposit accounts we created. I chose to do this a particular way, mainly due to how the the transactions history api works, in that it returns a log of all transactions created in some session, regardless of whether we have processed that message already or not. These are timestamped, and so logic can be created to ensure we are picking off just the messages that are more recent than a last read message. and that was certainly how i was about to do it. But i dont beleive this would be how these transactions would be sourced in a production environment. I imagine events of this nature would probably be consumed off a queue. 

Because of that, my realization was that we really only care about the deposit addresses we created for the users, and their balances. And so i decided to utilize the address balance api, and poll the balances of the deposit addresses i create. which i have kept in a makeshift "DB" (ConcurrentHashMap, more comments in code),
and for each depositAddress we keep the list of addresses to distribute funds to. so we divide the balance (if there is one) by the number of addresses and distribute the funds across them all.

Some questions i couldve had, but due to my schedule, pushed forward:
- do the transactions to distribute the funds need to distribute all the funds, or just move a little amount to each account over time? i implemented the former


