@Lenient
Feature: Similarity

  Scenario: Similarities same case
    Given Healed is "Healed"
     Then Healed should be 80% similar to "Sealed"
      And Healed should not be 81.01% similar to "Sealed"
      And Healed should not be 79.99% similar to "Sealed"
      And Healed should be 54.55% similar to "Healthy"
      And Healed should be less than 55% similar to "Healthy"
      And Healed should be at most 55% similar to "Healthy"
      And Healed should be more than 54% similar to "Healthy"
      And Healed should be at least 54% similar to "Healthy"
      And Healed should not be less than 54% similar to "Healthy"
      And Healed should not be at most 54% similar to "Healthy"
      And Healed should not be more than 55% similar to "Healthy"
      And Healed should not be at least 55% similar to "Healthy"
      And Healed should not be 55% similar to "Healthy"
      And Healed should be 44.44% similar to "Heard"
      And Healed should be less than 45% similar to "Heard"
      And Healed should be at most 45% similar to "Heard"
      And Healed should be more than 44% similar to "Heard"
      And Healed should be at least 44% similar to "Heard"
      And Healed should not be less than 44% similar to "Heard"
      And Healed should not be at most 44% similar to "Heard"
      And Healed should not be more than 45% similar to "Heard"
      And Healed should not be at least 45% similar to "Heard"
      And Healed should not be 44% similar to "Heard"
      And Healed should be 40% similar to "Herded"
      And Healed should be 25% similar to "Help"
      And Healed should be 0% similar to "Sold"

  Scenario: Similarities different case
    Given Healed is "Healed"
     Then Healed should be 80% similar to "sealed"
      And Healed should not be 80.01% similar to "sealed"
      And Healed should not be 79.99% similar to "sealed"
      And Healed should be 36.36% similar to "healthy"
      And Healed should be less than 37% similar to "healthy"
      And Healed should be at most 37% similar to "healthy"
      And Healed should be more than 36% similar to "healthy"
      And Healed should be at least 36% similar to "healthy"
      And Healed should not be less than 36% similar to "healthy"
      And Healed should not be at most 36% similar to "healthy"
      And Healed should not be more than 37% similar to "healthy"
      And Healed should not be at least 37% similar to "healthy"
      And Healed should not be 55% similar to "healthy"
      And Healed should be 22.22% similar to "heard"
      And Healed should be less than 23% similar to "heard"
      And Healed should be at most 23% similar to "heard"
      And Healed should be more than 22% similar to "heard"
      And Healed should be at least 22% similar to "heard"
      And Healed should not be less than 22% similar to "heard"
      And Healed should not be at most 22% similar to "heard"
      And Healed should not be more than 23% similar to "heard"
      And Healed should not be at least 23% similar to "heard"
      And Healed should not be 22% similar to "heard"
      And Healed should be 20% similar to "herded"
      And Healed should be 0% similar to "help"
      And Healed should be 0% similar to "sold"

  Scenario: Similarities ignoring case
    Given Healed is "Healed"
     Then Healed should be 80% similar to "sealed" ignoring case
      And Healed should be 80% similar to "Sealed" ignoring case
      And Healed should not be 80.01% similar to "sealed" ignoring case
      And Healed should not be 80.01% similar to "Sealed" ignoring case
      And Healed should not be 79.99% similar to "sealed" ignoring case
      And Healed should not be 79.99% similar to "Sealed" ignoring case
      And Healed should be 54.55% similar to "healthy" ignoring case
      And Healed should be 54.55% similar to "Healthy" ignoring case
      And Healed should be less than 55% similar to "healthy" ignoring case
      And Healed should be less than 55% similar to "Healthy" ignoring case
      And Healed should be at most 55% similar to "healthy" ignoring case
      And Healed should be at most 55% similar to "Healthy" ignoring case
      And Healed should be more than 54% similar to "healthy" ignoring case
      And Healed should be more than 54% similar to "Healthy" ignoring case
      And Healed should be at least 54% similar to "healthy" ignoring case
      And Healed should be at least 54% similar to "Healthy" ignoring case
      And Healed should not be less than 54% similar to "healthy" ignoring case
      And Healed should not be less than 54% similar to "Healthy" ignoring case
      And Healed should not be at most 54% similar to "healthy" ignoring case
      And Healed should not be at most 54% similar to "Healthy" ignoring case
      And Healed should not be more than 55% similar to "healthy" ignoring case
      And Healed should not be more than 55% similar to "Healthy" ignoring case
      And Healed should not be at least 55% similar to "healthy" ignoring case
      And Healed should not be at least 55% similar to "Healthy" ignoring case
      And Healed should not be 55% similar to "healthy" ignoring case
      And Healed should not be 55% similar to "Healthy" ignoring case
      And Healed should be 44.44% similar to "heard" ignoring case
      And Healed should be 44.44% similar to "Heard" ignoring case
      And Healed should be less than 45% similar to "heard" ignoring case
      And Healed should be less than 45% similar to "Heard" ignoring case
      And Healed should be at most 45% similar to "heard" ignoring case
      And Healed should be at most 45% similar to "Heard" ignoring case
      And Healed should be more than 44% similar to "heard" ignoring case
      And Healed should be more than 44% similar to "Heard" ignoring case
      And Healed should be at least 44% similar to "heard" ignoring case
      And Healed should be at least 44% similar to "Heard" ignoring case
      And Healed should not be less than 44% similar to "heard" ignoring case
      And Healed should not be less than 44% similar to "Heard" ignoring case
      And Healed should not be at most 44% similar to "heard" ignoring case
      And Healed should not be at most 44% similar to "Heard" ignoring case
      And Healed should not be more than 45% similar to "heard" ignoring case
      And Healed should not be more than 45% similar to "Heard" ignoring case
      And Healed should not be at least 45% similar to "heard" ignoring case
      And Healed should not be at least 45% similar to "Heard" ignoring case
      And Healed should not be 44% similar to "heard" ignoring case
      And Healed should not be 44% similar to "Heard" ignoring case
      And Healed should be 40% similar to "herded" ignoring case
      And Healed should be 40% similar to "Herded" ignoring case
      And Healed should be 25% similar to "help" ignoring case
      And Healed should be 25% similar to "Help" ignoring case
      And Healed should be 0% similar to "sold" ignoring case
      And Healed should be 0% similar to "Sold" ignoring case

  Scenario: Similarity references
    Given phrase 1 is "Hello world!"
      And phrase 2 is "Hello world"
      And phrase 3 is "hello world"
     Then phrase 1 should be more than 90% similar to phrase 2
      And phrase 1 should be at least 90% similar to phrase 2 ignoring case
      And phrase 1 should be 94.12% similar to phrase 2
      And phrase 1 should be 94.12% similar to phrase 3 ignoring case
      And phrase 1 should be 82.35% similar to phrase 3
      And phrase 1 should not be less than 90% similar to phrase 2
      And phrase 1 should be 100% similar to phrase 1

  Scenario: Similarity capture ref to text as name
    Given the phrase is "Hello world!"
     When I capture the similarity score of the phrase compared to "Hello world" as similarity score 1
      And I capture the similarity score of the phrase compared to "hello world" as similarity score 2
      And I capture the similarity score of the phrase compared to "hello world" ignoring case as similarity score 3
     Then similarity score 1 should be "0.9411764705882353"
      And similarity score 2 should be "0.8235294117647058"
      And similarity score 3 should be "0.9411764705882353"

  Scenario: Similarity capture ref to ref as name
    Given phrase 1 is "Hello world!"
      And phrase 2 is "hello world"
     When I capture the similarity score of phrase 1 compared to phrase 2 as similarity score 1
      And I capture the similarity score of phrase 1 compared to phrase 2 ignoring case as similarity score 2
     Then similarity score 1 should be "0.8235294117647058"
      And similarity score 2 should be "0.9411764705882353"

  Scenario: Similarity capture ref to text
    Given the phrase is "Hello world!"
     When I capture the similarity score of the phrase compared to "Hello world"
     Then similarity score should be "0.9411764705882353"

  Scenario: Similarity capture ref to ref
    Given phrase 1 is "Hello world!"
      And phrase 2 is "hello world"
     When I capture the similarity score of phrase 1 compared to phrase 2
     Then similarity score should be "0.8235294117647058"

  Scenario: Similarity capture same ref to ref
    Given phrase 1 is "Hello world!"
     When I capture the similarity score of phrase 1 compared to phrase 1
     Then similarity score should be "1.0"
