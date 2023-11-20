# ComputerSecurity

In order to run the application you must **first** start the server [Server.java](src/main/java/ClientServer/Server.java) and **then** the client [Client.java](src/main/java/ClientServer/Client.java).


To add a client create a `.json` file in the [clients](clients) folder. The file must contain the following information:

```json
{
  "id": "Client1",
  "password": "pass123",
  "server": {
    "ip": "127.0.0.1",
    "port": 12345
  },
  "actions": {
    "delay": 10,
    "steps": [
      "INCREASE 10",
      "DECREASE 5"
    ]
  }
}

```
