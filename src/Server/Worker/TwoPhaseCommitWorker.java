package Server.Worker;

import Model.Message;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class TwoPhaseCommitWorker implements Runnable {
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;
    private Object nextElement;
    private DataManager dataManager;

    public TwoPhaseCommitWorker(DataManager dataManager, ObjectInputStream serverIn, ObjectOutputStream serverOut) {
        this.serverIn = serverIn;
        this.serverOut = serverOut;
        this.dataManager = dataManager;
    }




    @Override
    public void run() {
        while(true) {
            try {
                nextElement = serverIn.readObject();
                if(nextElement instanceof Message) {
                    Message myMessage = (Message) nextElement;
                    switch(myMessage.getStatus()) {
                        case "PREPARE":
                            dataManager.writeLogEntry(System.currentTimeMillis() + " - testing if message " + (myMessage.getHeader().getMessageId()) + " can be committed locally");
                            if(dataManager.messageCanBeCommited(myMessage)) {
                                myMessage.setStatus("READY");
                                dataManager.writeLogEntry(System.currentTimeMillis() + " - message " + (myMessage.getHeader().getMessageId()) + " can be committed locally");
                            } else {
                                myMessage.setStatus("ABORT");
                                dataManager.writeLogEntry(System.currentTimeMillis() + " - message " + (myMessage.getHeader().getMessageId()) + " can not be committed locally");
                            }
                            break;
                        case "COMMIT":
                            dataManager.writeMessage(myMessage);
                            dataManager.writeLogEntry(System.currentTimeMillis() + " - message " + (myMessage.getHeader().getMessageId()) + " committed successfully");
                            myMessage.setStatus("OK");
                            break;
                        case "ABORT":
                            dataManager.abortMessage(myMessage);
                            dataManager.writeLogEntry(System.currentTimeMillis() + " - message " + (myMessage.getHeader().getMessageId()) + " aborted successfully");
                            myMessage.setStatus("OK");
                            break;
                    }
                    serverOut.writeObject(myMessage);
                    serverOut.flush();
                }

            } catch(IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
        }
    }
}
