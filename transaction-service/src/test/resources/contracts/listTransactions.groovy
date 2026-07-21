package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should list transactions with pagination"
    
    request {
        method GET()
        url '/api/v1/transactions' {
            queryParameters {
                parameter 'page': 0
                parameter 'size': 10
                parameter 'merchantId': $(consumer(optional(regex('[A-Z0-9_]+'))), producer('MERCHANT_12345'))
            }
        }
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
                content: [
                        [
                                transactionId : $(consumer(~/.+/), producer("TX_001")),
                                merchantId    : fromRequest().query('merchantId'),
                                amount        : 99.99,
                                currency      : "USD",
                                status        : "APPROVED",
                                createdAt     : $(consumer(~/.+/), producer("2024-01-01T12:00:00Z"))
                        ],
                        [
                                transactionId : $(consumer(~/.+/), producer("TX_002")),
                                merchantId    : fromRequest().query('merchantId'),
                                amount        : 149.99,
                                currency      : "USD",
                                status        : "PENDING",
                                createdAt     : $(consumer(~/.+/), producer("2024-01-01T13:00:00Z"))
                        ]
                ],
                pageable: [
                        pageNumber: 0,
                        pageSize  : 10
                ],
                totalElements: 2,
                totalPages   : 1,
                last         : true
        ])
    }
}
