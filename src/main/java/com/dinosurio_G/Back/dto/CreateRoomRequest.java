package com.dinosurio_G.Back.dto;

public class CreateRoomRequest {
    private String roomName;
    private int maxPlayers;
    private String hostName;

    public CreateRoomRequest() {}

    public CreateRoomRequest(String roomName, int maxPlayers, String hostName) {
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        this.hostName = hostName;
    }

    // Getters y Setters
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
}
