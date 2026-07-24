# Benchmark Summary

| variant | resource_profile | status | technical_valid | technical_reason | block_id | block_position | outcome_completeness | finished_outcomes | error_outcomes | technical_status_lost_outcomes | confirmed_business_throughput_per_second | transport_requests_ko | transport_p95_response_ms | sut_cpu_mean_cores | sut_memory_mean_bytes | run_id |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| monolith | vertical-2 | passed | true |  | NULL | NULL | not_applicable | 0 | 0 | 0 | 0.000000 | 0 | 381 | 0.295794 | 825466880.000000 | 20260724T164038Z-thesis-smoke-monolith |
| modular_monolith | vertical-2 | passed | true |  | NULL | NULL | not_applicable | 0 | 0 | 0 | 0.000000 | 0 | 254 | 0.237888 | 703946752.000000 | 20260724T164256Z-thesis-smoke-modular |
| microservices | vertical-2 | passed | true |  | NULL | NULL | not_applicable | 0 | 0 | 0 | 0.000000 | 0 | 715 | 0.261124 | 1024234496.000000 | 20260724T164505Z-thesis-smoke-microservices |
| monolith | vertical-2 | passed | true |  | NULL | NULL | complete | 8 | 0 | 0 | 0.139668 | 0 | 401 | 1.442606 | 926986922.666667 | 20260724T164858Z-thesis-load-monolith |
| modular_monolith | vertical-2 | passed | true |  | NULL | NULL | complete | 8 | 0 | 0 | 0.139948 | 0 | 206 | 1.390301 | 982054912.000000 | 20260724T165154Z-thesis-load-modular |
| microservices | vertical-2 | passed | true |  | NULL | NULL | complete | 8 | 0 | 0 | 0.091585 | 0 | 14 | 0.813649 | 1420592924.444444 | 20260724T165447Z-thesis-load-microservices |
| microservices | vertical-2 | passed | true |  | NULL | NULL | complete | 8 | 0 | 0 | 0.087949 | 0 | 77 | 0.864612 | 1289256744.421053 | 20260724T170523Z-thesis-load-microservices-metricgate |
| microservices | vertical-2 | passed | true |  | NULL | NULL | complete | 8 | 0 | 0 | 0.094633 | 0 | 69 | 0.813053 | 1235493104.941176 | 20260724T172126Z-thesis-load-microservices-semanticgate |
