package com.superzanti.serversync.communication;

import com.superzanti.serversync.client.ActionEntry;
import com.superzanti.serversync.client.ActionProgress;
import com.superzanti.serversync.client.Server;
import com.superzanti.serversync.communication.response.ServerInfo;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EServerMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Requests {
    private final Server server;
    private final ServerInfo info;
    private final ObjectInputStream input;
    private final ObjectOutputStream output;

    private Requests(Server server) {
        this.server = server;
        this.info = server.info;
        this.input = server.input;
        this.output = server.output;
    }

    public static Requests forServer(Server server) {
        return new Requests(server);
    }

    /**
     * The manifest of files present on the server.
     *
     * @return The file manifest or null if an error occurs
     */
    public FileManifest getManifest() {
        Logger.debug("Requesting file manifest");
        try {
            writeMessage(EServerMessage.GET_MANIFEST);
        } catch (IOException e) {
            return null;
        }

        try {
            return (FileManifest) input.readObject();
        } catch (ClassNotFoundException | IOException e) {
            Logger.debug("Failed to read from server stream");
            Logger.debug(e);
            return null;
        }
    }

    public List<String> getManagedDirectories() {
        Logger.debug("Requesting managed directories");
        try {
            writeMessage(EServerMessage.GET_MANAGED_DIRECTORIES);
        } catch (IOException e) {
            Logger.debug("Failed to write message to server");
            Logger.debug(e);
            return Collections.emptyList();
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> directories = (List<String>) input.readObject();
            return directories;
        } catch (ClassNotFoundException e) {
            Logger.debug("Received unknown object in server response");
            Logger.debug(e);
            return Collections.emptyList();
        } catch (IOException e) {
            Logger.debug("Failed to read from server stream");
            Logger.debug(e);
            return Collections.emptyList();
        }
    }

    /**
     * The number of files being managed by the server.
     *
     * @return int number of files or -1 if failure occurs
     */
    public int getNumberOfManagedFiles() {
        Logger.debug("Requesting number of managed files");
        try {
            writeMessage(EServerMessage.GET_NUMBER_OF_MANAGED_FILES);
        } catch (IOException e) {
            Logger.debug("Failed to write to server stream");
            Logger.debug(e);
            return -1;
        }

        try {
            return input.readInt();
        } catch (IOException e) {
            Logger.debug("Failed to read from server stream");
            Logger.debug(e);
            return -1;
        }
    }

    public boolean updateFile(ActionEntry entry, Consumer<ActionProgress> progressConsumer) {
        try {
            writeMessage(EServerMessage.UPDATE_FILE);
            writeObject(entry.target);
        } catch (IOException e) {
            return false;
        }

        try {
            boolean serverHasFile = input.readBoolean();

            if (!serverHasFile) {
                Logger.error(String.format("File does not exist on the server: %s", entry.target));
                return false;
            }
        } catch (IOException e) {
            Logger.error("Failed to read file status from stream");
            Logger.debug(e);
            return false;
        }

        try {
            long fileSize = input.readLong();
            ActionProgress progress = new ActionProgress(-1d, entry.target.path, false, entry);
            progressConsumer.accept(progress);// Init the updates with baseline

            SyncFileOutputStream out = new SyncFileOutputStream(server, fileSize, entry.target.resolvePath());
            out.write((pc) -> {
                progress.setProgress(pc);
                progressConsumer.accept(progress);
            });
            progress.setComplete(true);
            progressConsumer.accept(progress);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void writeMessage(EServerMessage message) throws IOException {
        String m = message.toString();
        try {
            output.writeUTF(m);
            output.flush();
        } catch (IOException e) {
            Logger.debug(String.format("Failed to write message %s to server stream", message));
            Logger.debug(e);
            throw e;
        }
    }

    private void writeObject(Object o) throws IOException {
        try {
            output.writeObject(o);
            output.flush();
        } catch (IOException e) {
            Logger.debug("Failed to write to server stream");
            Logger.debug(e);
            throw e;
        }
    }
}
