package src;
import com.google.gson.Gson;
import com.mysql.cj.PreparedQuery;

import java.net.*;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Server {
    final int port = 12345;
    public ArrayList<User> connectedClients = new ArrayList<User>();
    private ServerSocket serverSocket;

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        ExecutorService executor = Executors.newCachedThreadPool();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleClient(clientSocket));
        }
    }

    private void handleClient(Socket socket) {
        User session = null;
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("CLIENT CONNECTED " + socket);
            session = new User();
            session.isSignedIn=false;
            session.clientSocket=socket;
            connectedClients.add(session);

            output.writeUTF("CONNECTED");
            String receivedString;

            while (true) {
                receivedString = input.readUTF();
                if (receivedString.equals("Exit")) {
                    System.out.println("Client " + socket + " sends exit...");
                    System.out.println("Connection closing...");
                    socket.close();
                    System.out.println("Closed");
                    if (session != null)
                    {
                        connectedClients.remove(session);
                        session = null;
                    }
                    break;
                }else if (!receivedString.equals("ping"))
                {
                    NetData data = NetData.fromJSON(receivedString);
                    if (data != null)
                    {
                        switch (data.operation)
                        {
                            case Register:
                            {
                                System.out.println("Received Register Request from " + socket + " with DATA: " + data.toJSON());
                                NetData response = new NetData(NetData.Operation.Register);
                                if (session.isSignedIn)
                                {
                                    SendError(output, "Jestes juz zalogowany!", response);
                                }
                                else
                                {
                                    if (data.Strings.size() == 5) //czy jest 5 stringow
                                    {
                                        if (data.Integers.size()==1) //czy podano nr telefonu w requestcie
                                        {
                                            int numtel = data.Integers.get(0);
                                            if (String.valueOf(numtel).length() == 9) // czy 9 znakow na numerze telefonu
                                            {
                                                for(String s : data.Strings) // sprawdzenie czy ktorys ze stringow jest pusty
                                                {
                                                    if (s.isEmpty())
                                                    {
                                                        SendError(output, "Zadne pole nie moze byc puste!", response);
                                                    }
                                                }
                                                if (data.Strings.get(1).equals(data.Strings.get(2))) // czy takie same hasła
                                                {
                                                    String existsquery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE `login` = \"" + data.Strings.get(0) + "\" OR numer_telefonu = " + numtel + ";";
                                                    DatabaseHandler dbh = new DatabaseHandler();
                                                    if (dbh == null || dbh.conn.isClosed())
                                                    {
                                                        SendError(output, "Blad polaczenia z baza danych.", response);
                                                        dbh.close();
                                                        break;
                                                    }
                                                    ResultSet existing = dbh.executeQuery(existsquery);
                                                    try {
                                                        if (existing.isClosed() || existing.next())
                                                        {
                                                            SendError(output, "Istnieje juz uzytkownik o takim loginie lub numerze telefonu, lub wystapil nieznany blad polaczenia z baza danych!", response);
                                                            break;
                                                        }
                                                        else
                                                        {
                                                            String registerQuery = "INSERT INTO uzytkownicy(`login`,`password`,`imie`,`nazwisko`,`data_utworzenia`,`numer_telefonu`,`typy_uzytkownikow_id_typu`) VALUES (\""+data.Strings.get(0).trim()+"\", \""+data.Strings.get(1).trim()+"\", \""+data.Strings.get(3).trim()+"\", \""+data.Strings.get(4).trim()+"\", NOW(), " + numtel+", 1);";
                                                            int registerResult = dbh.executeUpdate(registerQuery);
                                                            if(registerResult>0)
                                                            {
                                                                response.operationType = NetData.OperationType.Success;
                                                                output.writeUTF(response.toJSON());
                                                                output.flush();
                                                                break;
                                                            }
                                                            else
                                                            {
                                                                SendError(output, "Nie udalo sie zarejestrowac.", response);
                                                                break;
                                                            }
                                                        }
                                                    } catch (SQLException e) {
                                                        e.printStackTrace();
                                                    } finally {
                                                        try {
                                                            existing.close();
                                                        } catch (SQLException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                        dbh.close();
                                                    }
                                                }
                                            }else
                                            {
                                                SendError(output, "Numer telefonu jest nieprawidlowy!", response);
                                                break;
                                            }
                                        }
                                        else
                                        {
                                            SendError(output, "Brak numeru telefonu!", response);
                                            break;
                                        }
                                    }
                                    else
                                    {
                                        SendError(output, "Brakuje jakiegos pola!", response);
                                        break;
                                    }
                                }
                            }
                            default:
                            {
                                System.out.println(socket + " requested unknown operation.");
                                break;
                            }
                        }
                    }
                    else
                    {
                        System.out.println("ERROR TRANSLATING DATA FROM " + socket);
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Client " + socket + " unexpectedly closed the connection.");
            if (session != null)
            {
                connectedClients.remove(session);
                session = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (session != null)
            {
                connectedClients.remove(session);
                session = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally
        {
            if (session != null)
            {
                connectedClients.remove(session);
                session = null;
            }
        }
    }
    void SendError(DataOutputStream output, String error, NetData data) throws IOException {
        if (data == null || output == null)
            return;
        data.operationType= NetData.OperationType.Error;
        data.Strings.add(error);
        output.writeUTF(data.toJSON());
        output.flush();
    }
    void SendMessage(DataOutputStream output, String mes, NetData data) throws IOException {
        if (data == null || output == null)
            return;
        data.operationType= NetData.OperationType.MessageBox;
        data.Strings.add(mes);
        output.writeUTF(data.toJSON());
        output.flush();
    }
}
class User
{
    public boolean isSignedIn;
    public Socket clientSocket;
}
