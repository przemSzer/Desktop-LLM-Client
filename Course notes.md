# Course ideas

Here are some ideas for the course about LLM for Java developers.
The course will teach Java developers how to use LLMs in their projects using langchain4j.
It will be based on a application which will be developed during the course.
The target audience is Java developers who want to learn how to use LLMs in their projects, they are expirienced with Java 
and I want to give them a good understanding of LLM, so that they can quickly use them in their projects.

## LLM concepts

* LLM concepts:
    ** always provide some comparision to existing developer tools
    *** LLM is a tool like database it is not a magic, 
    *** database is a complicated llm also

## Feature Comparison

| Feature | LLM | DBMS |
|---------|-----|------|
| Data Storage | Context window, memory | Tables, indexes |
| Query Language | Natural language (Prompts) | SQL |
| Performance | Variable response time | Optimized queries |
| Consistency | Probabilistic | ACID properties |
| Integration | API calls | JDBC, JPA |
| Cost | Per-token pricing | License + infrastructure (hardware) |
| Use Cases | Text generation, analysis, extracting information from text and many more| Data persistence, maintenance, retrieval |


## LLM
 
* connecting to a LLM (local, Open AI)
    ** task write a code which will connect to a LLM and send a message
* showing simple conversation with LLM,
* showing memory concept,
* message types: system, user, ai and their role
* streaming response from LLM,
* using different model parameters per request
* explaining context concept, and stateless concept
* some additional topics:
    ** prompt caching
* prompt engineering

## Testing

* unit tests for our solution
* integration tests for langChain4j
* end to end tests for langChain4j
* llm tools and llm utils

## Production staff

* resilience build into langChain4j
    * timeouts
    * retries
* monitoring and logging build into langChain4j
* rate limiting for OpenAI clients

# Production deployment

* deployment to production
* monitoring and logging
* rate limiting for OpenAI clients
* security
* data privacy

# Production monitoring

* monitoring and logging
* rate limiting for OpenAI clients

# Example apps

General idea topic about how LLMs can be used.

* summarisation,
* information extraction,
* code generation,
* question answering,
* text completion,
* image generation,
* voice generation,
* voice recognition,
* sentiment analysis,

## Advanced topics

### RAG (knowledge base)

### Flows

### Agents




