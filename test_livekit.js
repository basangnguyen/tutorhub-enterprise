const { Room } = require('livekit-client');

async function test() {
    try {
        console.log("Connecting...");
        const room = new Room();
        const token = "eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoidGVzdCIsInZpZGVvIjp7InJvb21Kb2luIjp0cnVlLCJyb29tIjoidGVzdCJ9LCJpc3MiOiJBUElYdGJNeUJBTW1qZ0ciLCJleHAiOjE3ODMzMzE1OTgsIm5iZiI6MTc4MzMwOTk5OCwic3ViIjoiaWRfNDA3MDY1In0.IFdKMrS2jYpLWiK0pMJCV1WyIzVlxVeM9gAhNf1Rf4Y";
        await room.connect('wss://tutorhub-enterprise-q820cqx7.livekit.cloud', token);
        console.log("Connected successfully!");
        room.disconnect();
    } catch (e) {
        console.error("Error connecting:", e);
    }
}
test();
