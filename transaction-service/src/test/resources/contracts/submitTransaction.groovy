package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should accept a valid transaction submission"
    
    request {
        method POST()
        url "/api/v1/transactions"
        headers {
            contentType(applicationJson())
        }
        body([
                merchantId    : "MERCHANT_12345",
                amount        : 99.99,
                currency      : "USD",
                cardNumber    : "4111111111111111",
                cardholderName: "John Doe",
                cardExpiry    : "12/25",
                cardCvv       : "123"
        ])
    }
    
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body([
                transactionId : $(consumer(~/.+/), producer("TX_${UUID.randomUUID().toString()}")),
                merchantId    : fromRequest().body('$.merchantId'),
                amount        : fromRequest().body('$.amount'),
                currency      : fromRequest().body('$.currency'),
                status        : "PENDING",
                createdAt     : $(consumer(~/.+/), producer("${new Date().toInstant().toString()}"))
        ])
    }
}
