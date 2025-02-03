# Repository for AsianCuisine backend
# Features
## Tag-based Recommendation System
- Version 1.0
  - Method: User tags and popular platform tags are extracted one at a time and then searched using ES and paged using ES's built-in paging system
  - Cons: The recommended paging content is based on the same set of tags each time the user refreshes, so it is not possible to get the content of other tags in a single refresh.
- Version 2.0
  - All tags are given weights based on the number of visits to the platform, sorted based on relevance using ES's socreFunction, and the local storage Cursor indicates the current paging result, so that the user's content is not duplicated.
  - Cons: As the depth of the Cursor increases, the search performance of ES decreases significantly and the relevance of the content obtained by the user continues to decline.
- Version 3.0
  - Used tag to give weight based on the number of visits to the platform, but adds a random score to make the recommended content diverse while ensuring relevance. And BloomFilter is used to filter duplicate content. And every time you refresh the data will reset the cursor, reducing the depth of the ES search.
## High-performance Post Metric Updating Mechanism
- Used Redis Bitmap to efficiently track posts requiring cache updates, enabling batch processing
- Created a scheduled scanner to identify posts marked for updates in the Bitmap
- Introduced asynchronous message queues for batch updates, reducing database load and improving task throughput