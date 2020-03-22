public class Chat{
    private String chatId;
    private Client particapteA;
    private Client particapteB;

    public Chat(Client particapteA, Client particapteB) {
        chatId = generateChatId();
        this.particapteA = particapteA;
        this.particapteB = particapteB;
    }

    private String generateChatId() {
        return "";
    }
}