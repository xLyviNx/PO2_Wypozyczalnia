package src;
import com.google.gson.Gson;
import com.mysql.cj.PreparedQuery;

import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                                    break;
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
                                                    if (dbh.conn == null || dbh.conn.isClosed())
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
                                                                session.isSignedIn=true;
                                                                session.username = data.Strings.get(0);
                                                                dbh.close();
                                                                break;
                                                            }
                                                            else
                                                            {
                                                                SendError(output, "Nie udalo sie zarejestrowac.", response);
                                                                dbh.close();
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
                            case Login:
                            {
                                System.out.println("Received LOGIN Request from " + socket + " with DATA: " + data.toJSON());
                                NetData response = new NetData(NetData.Operation.Login);
                                if (session.isSignedIn)
                                {
                                    SendError(output, "Jestes juz zalogowany!", response);
                                    break;
                                }
                                else {
                                    if (data.Strings.size() == 2) //czy jest 2 stringow
                                    {
                                        for (String s : data.Strings) // sprawdzenie czy ktorys ze stringow jest pusty
                                        {
                                            if (s.isEmpty()) {
                                                SendError(output, "Zadne pole nie moze byc puste!", response);
                                            }
                                        }

                                        String existsquery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE `login` = \"" + data.Strings.get(0) + "\" AND password = \"" + data.Strings.get(1) + "\";";
                                        DatabaseHandler dbh = new DatabaseHandler();
                                        if (dbh.conn == null || dbh.conn.isClosed()) {
                                            SendError(output, "Blad polaczenia z baza danych.", response);
                                            dbh.close();
                                            break;
                                        }
                                        //System.out.println(existsquery);
                                        ResultSet existing = dbh.executeQuery(existsquery);
                                        try {
                                            if (!existing.isClosed() && existing.next())
                                            {
                                                session.isSignedIn=true;
                                                session.username=data.Strings.get(0);
                                                response.operationType = NetData.OperationType.Success;
                                                output.writeUTF(response.toJSON());
                                                output.flush();
                                                dbh.close();
                                                break;
                                            }
                                            else
                                            {
                                                SendError(output, "Nie udalo sie zalogowac! Upewnij sie ze dane sa poprawne!", response);
                                                dbh.close();
                                                break;
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
                                }
                                break;
                            }
                            case OfferUsername:
                            {
                                NetData response = new NetData(NetData.Operation.OfferUsername);
                                response.Booleans.add(session.isSignedIn);
                                if (session.isSignedIn)
                                {
                                    response.Strings.add(session.username);
                                }
                                else
                                {
                                    response.Strings.add("NIEZALOGOWANY");
                                }
                                output.writeUTF(response.toJSON());
                                output.flush();
                                break;
                            }
                            case OfferElement:
                            {
                                String query = "SELECT * FROM `auta` ORDER BY `cenaZaDzien` ASC;";
                                DatabaseHandler dbh = new DatabaseHandler();
                                if (dbh.conn == null || dbh.conn.isClosed())
                                {
                                    NetData response = new NetData(NetData.Operation.OfferElement);
                                    SendError(output, "Błąd połączenia z bazą danych.", response);
                                    dbh.close();
                                    break;
                                }
                                ResultSet result = dbh.executeQuery(query);
                                while(result.next())
                                {
                                    System.out.println("SENDING AN OFFER.");
                                    NetData response = new NetData(NetData.Operation.OfferElement);
                                    int id = result.getInt("id_auta");
                                    String marka = result.getString("marka");
                                    String model = result.getString("model");
                                    String silnik = result.getString("silnik");
                                    int prod = result.getInt("rok_prod");
                                    float cena = result.getFloat("cenaZaDzien");
                                    String topText = marka + " " + model + " (" + prod + ") " + silnik;
                                    response.Strings.add(topText);
                                    response.Floats.add(cena);
                                    response.Integers.add(id);
                                    String zdjecie = result.getString("zdjecie");
                                    if (!zdjecie.isEmpty())
                                        response.Images.add(loadImageAsBytes(zdjecie));

                                    output.writeUTF(response.toJSON());
                                    output.flush();
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
    private static byte[] loadImageAsBytes(String imagePath) {
        try {
            InputStream stream = Server.class.getResourceAsStream(imagePath);
            if (stream != null) {
                return stream.readAllBytes();
            } else {
                // Handle the case where the resource is not found
                System.out.println("Resource not found: " + imagePath);
                return new byte[0];
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (e.g., log it or return a default image)
            return new byte[0]; // Return an empty byte array as a placeholder
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
    public String username;
    public Socket clientSocket;
}
