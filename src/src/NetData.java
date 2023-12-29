package src;

import com.google.gson.Gson;

import java.util.ArrayList;

public class NetData
{
    boolean success;
    Operation operation;
    ArrayList<String> Strings = new ArrayList<String>();
    ArrayList<Integer> Integers= new ArrayList<Integer>();
    ArrayList<Boolean> Booleans= new ArrayList<Boolean>();
    ArrayList<Byte[]> Images = new ArrayList<>();

    public NetData(Operation op, String str)
    {
        operation = op;
        Strings.add(str);
        System.out.println(toJSON());
    }
    public NetData(Operation op)
    {
        operation = op;
        System.out.println(toJSON());
    }
    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static NetData fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, NetData.class);
    }
    public static enum Operation
    {
        Unspecified,
        Register,
        Login,
    }
}
