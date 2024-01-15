import json
import random
import time
import zipfile
from kafka import KafkaProducer


# Define a function to send a batch of tweets to Kafka
def send_tweets_bunch(tweets, producer):
    for tweet in tweets:
        producer.send('tweets', value=tweet)
    producer.flush()


zip_file_path = 'C:\\Users\\Think\\OneDrive\\Documents\\BigData\\Project\\archive.zip'

# Initialize a KafkaProducer with the bootstrap servers and a custom value serializer for JSON
producer = KafkaProducer(bootstrap_servers=['localhost:9092'],
                         value_serializer=lambda v: json.dumps(v).encode('utf-8'))

with zipfile.ZipFile(zip_file_path, 'r') as z:
    # Open the first file inside the ZIP archive
    with z.open(z.namelist()[0]) as f:
        tweets_batch = []

        # Iterate through each line in the file
        for line in f:
            # Decode the line from bytes to utf-8 and strip any whitespaces
            line = line.decode("utf-8").strip()

            # Split the line into attributes using commas
            attribute_details = line.split(',')
            tweet = {
                "id": attribute_details[1].strip('"'),
                "date": attribute_details[2].strip('"'),
                "user": attribute_details[4].strip('"'),
                "text": attribute_details[5].strip('"'),
                "retweets": int(random.random() * 10)
            }

            # Add the tweet to the batch
            tweets_batch.append(tweet)

            # If the batch size reaches 100, send the batch to Kafka and reset the batch
            if len(tweets_batch) == 100:
                send_tweets_bunch(tweets_batch, producer)
                tweets_batch = []
            time.sleep(0.01)

# If there are remaining tweets in the batch, send them to Kafka
if tweets_batch:
    send_tweets_bunch(tweets_batch, producer)

# Close the KafkaProducer
producer.close()