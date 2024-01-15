import pandas as pd
import plotly.express as px
import plotly.io as pio
from flask import Flask, render_template, jsonify, request
from pymongo import MongoClient

# Initializing Flask application
app = Flask(__name__)

# Connecting to MongoDB
client = MongoClient('mongodb://localhost:27017/')
db = client['tweets']
collection = db['data']


# Function to generate a pie chart showing the top 20 users based on tweet count
def generate_top_users_pie_chart():
    # MongoDB aggregation pipeline to get user-wise tweet count
    pipeline = [
        {"$group": {"_id": "$user", "tweet_count": {"$sum": 1}}},
        {"$sort": {"tweet_count": -1}},
        {"$limit": 20}
    ]
    result = list(collection.aggregate(pipeline))

    # Creating a DataFrame from the MongoDB result
    df = pd.DataFrame(result)

    # Generating a pie chart using Plotly Express
    fig = px.pie(df, names="_id", values="tweet_count",
                 title="Top 20 Users in terms of the number of published tweets.")
    return fig


# Function to generate the default chart showing tweet distribution over time
def generate_tweet_distribution_trend_chart():
    # MongoDB aggregation pipeline to get daily tweet count
    pipeline = [
        {"$group": {"_id": {"$dateToString": {"format": "%Y-%m-%d", "date": "$date"}}, "tweet_count": {"$sum": 1}}},
        {"$sort": {"_id": 1}}
    ]
    result = list(collection.aggregate(pipeline))

    # Creating a DataFrame from the MongoDB result
    df = pd.DataFrame(result)

    # Generating a line chart using Plotly Express
    fig = px.line(df, x="_id", y="tweet_count", title="Tweet Distribution Over Time")
    return fig


# Function to generate a line chart showing tweet distribution over time for a specific user
def generate_filtered_tweet_distribution_chart(user_query):
    # MongoDB aggregation pipeline to get daily tweet count for a specific user
    pipeline = [
        {"$match": {"user": user_query}},
        {"$group": {"_id": {"$dateToString": {"format": "%Y-%m-%d", "date": "$date"}}, "tweet_count": {"$sum": 1}}},
        {"$sort": {"_id": 1}}
    ]
    result = list(collection.aggregate(pipeline))

    # Creating a DataFrame from the MongoDB result
    df = pd.DataFrame(result)

    # Generating a line chart using Plotly Express with user-specific title
    fig = px.line(df, x="_id", y="tweet_count", title=f"Tweet Distribution Over Time for User: {user_query}")
    return fig


# Route for the home page
@app.route('/')
def index():
    return render_template('index.html')


# Route to get the JSON representation of the top users pie chart
@app.route('/top_users_pie_chart')
def top_users_pie_chart():
    fig = generate_top_users_pie_chart()
    fig_json = pio.to_json(fig)
    return jsonify(fig_json)


# Route to get the JSON representation of the overall tweet distribution chart
@app.route('/tweets_distribution_chart')
def tweets_distribution_chart():
    fig = generate_tweet_distribution_trend_chart()
    fig_json = pio.to_json(fig)
    return jsonify(fig_json)


# Route to get the JSON representation of the tweet distribution chart for a specific user
@app.route('/filtered_tweets_distribution_chart', methods=['GET', 'POST'])
def filtered_tweets_distribution_chart():
    if request.method == 'POST':
        user_query = request.form.get('user_query', '')
        fig = generate_filtered_tweet_distribution_chart(user_query)
    else:
        # If it's a GET request, show the overall tweet distribution chart
        fig = generate_tweet_distribution_trend_chart()

    # Convert the Plotly figure to JSON and return
    fig_json = pio.to_json(fig)
    return jsonify(fig_json)


# Running the application
if __name__ == '__main__':
    app.run(debug=True)