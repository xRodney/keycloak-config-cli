# TODOs

## Implement

- https://github.com/keycloak/keycloak/pull/7780

# Tests

## Realm operator

* simple success
* keycloak not available -> retry
* keycloak connection secret does not exist -> retry (maybe hook at the secret creation?)

## Client operator

* simple success
* references realm resource that does not exist -> keep retrying until it does
* references realm resource that is not successfully imported -> keep retrying until it is
* references client secret that does not exist or does not contain the key -> keep retrying (maybe hook at the secret?)
* references client r
