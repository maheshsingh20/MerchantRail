package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should retrieve an existing transaction by ID"
    
    request {
        method GET()
        url $(consumer(~'/api/v1/transactions/[a-zA-Z0-9_-]+'), producer('/api/v1/transactions/TX_123456'))
        headers {
            accept(applicationJson())
        }
    }
    
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
                transactionId : fromRequest().path(4),
                merchantId    : "MERCHANT_12345",
                amount        : 99.99,
                currency      : "USD",
                status        : "APPROVED",
                cardLast4     : "1111",
                cardholderName: "John Doe",
                createdAt     : $(consumer(~/.+/), producer("2024-01-01T12:00:00Z")),
                updatedAt     : $(consumer(~/.+/), producer("2024-01-01T12:01:00Z"))
        ])
    }
}
