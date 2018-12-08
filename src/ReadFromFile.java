import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ReadFromFile {
	
	public static  List<HashMap<String,String>> listUsers(String csvFile, String delimeter, HashMap<String,String> attributeNames) {
        BufferedReader br = null;
        String line = "";
        HashMap<String, String> attributeMap = new HashMap<String, String>();
        List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
        StringBuilder invalidRecords = new StringBuilder();
        System.out.println("Input File to Read :::  "+csvFile);
        System.out.println("Validation report would be generated after loading of input file to : "+csvFile+".ERROR");
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                String[] row = line.split(delimeter);
                try{
                	int i =1;
                	if(null != row && row.length ==  attributeNames.values().size())
                	{
                		attributeMap = new HashMap<String, String>();
                		for(String attribute : row)
                		{  
                			
                			attributeMap.put(i+"", attribute);
                			i++;
                		}
                		data.add(attributeMap);
                	}
                	else
                	{
                		invalidRecords.append(line);
                		invalidRecords.append("\n");
                	}
                		
                }
                catch(Exception e)
                {
                    System.out.println("Failed to read record --> " + line +", Error Message --> "+e.getMessage());
                    invalidRecords.append(e.getMessage());
                }
            }

        } catch (FileNotFoundException e) {
            invalidRecords.append(e.getMessage());

            e.printStackTrace();
        } catch (IOException e) {
            invalidRecords.append(e.getMessage());

            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if( null != invalidRecords && invalidRecords.length() >0)
        {
        	usingBufferedWritter(csvFile+".ERROR", invalidRecords.toString());	
        }
        return data;

    }
	public static void usingBufferedWritter(String file, String data) 
	{
	     
	    BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			writer.write(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
