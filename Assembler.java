
	import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

	public class Assembler {
		private HashMap<String, Integer> hash = new HashMap<String, Integer>();
		
		public void writeOutToFile(String fIn, ArrayList<String> output, String fOut) {

			BufferedWriter bWriter = null;
			BufferedReader bReader = null;
			try {
				FileWriter fWriter = new FileWriter(fOut);

				bWriter = new BufferedWriter(fWriter);
				FileReader fReader = new FileReader(fIn);
				bReader = new BufferedReader(fReader);
				int i = 0;
				int offset = 256;
				while (i < output.size() && bReader.ready()) {

					String line = bReader.readLine() + "\t" + offset + "\t"
							+ output.get(i) + "\n";
					i++;
					bWriter.append(line);
					offset = offset + 4;
				}

			} catch (IOException e) {

				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (bWriter != null && bReader != null) {
						bWriter.close();
						bReader.close();
					}
				} catch (IOException ex) {

					ex.printStackTrace();
				}
			}

		}
		
		public ArrayList<String> FillInArrayList(String input){
			hash.put("010000", 1);
			 hash.put("010001", 2);
			 hash.put("010010", 3);
			 hash.put("010011", 4);
			 hash.put("010100", 5);
			 hash.put("010101", 6);
			 hash.put("010110", 7);
			 hash.put("010111", 8);
			 hash.put("011000", 9);
			 hash.put("011001", 10);
			 hash.put("011010", 11);
			 hash.put("011011", 12);
			 hash.put("110000", 13);
			 hash.put("110001", 14);
			 hash.put("110010", 15);
			 hash.put("110011", 16);
			 hash.put("110100", 17);
			 hash.put("110101", 18);
			 hash.put("110110", 19);
			 hash.put("110111", 20);
			 hash.put("111000", 21);
			 hash.put("111001", 22);
			 hash.put("111010", 23);
			 hash.put("111011", 24);
				BufferedReader br = null;
				ArrayList<String> decodedCode = new ArrayList<String>();
						try {
					String line;
					br = new BufferedReader(new FileReader(input));
					String decode;
					Integer data;
					boolean flag = false;
					char[] dataChar;
		 
					while ((line = br.readLine()) != null) {
						if(!flag){
						decode = interpret(line);
						decodedCode.add(decode);
						if("BREAK".equals(decode)){
							flag = true;
							continue;
						}				
					}
						else{
							if(line.charAt(0)=='1'){
								dataChar=line.toCharArray();
								for(int i=0;i<line.length();i++)
									dataChar[i] = (dataChar[i]=='0') ? '1':'0';
								  line = new String(dataChar);
								  data = Integer.parseInt(line,2);
								  data = -1 * (data+1);
									decodedCode.add(data.toString());
							}else{
								data = Integer.parseInt(line,2);
								decodedCode.add(data.toString());
							}
						}
					}
		 
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (br != null)br.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
						return decodedCode;
			}
		
		public String interpret(String s){
			String instrCode = s.substring(0, 6);
	if(hash.containsKey(instrCode)){
			switch(hash.get(instrCode)){
			case 1:
				return J(s);
			case 2:
				return JR(s);
			case 3:
				return BEQ(s);
			case 4:
				return BLTZ(s);
			case 5:
				return BGTZ(s);
			case 6:
				return BREAK(s);
			case 7:
				return SW(s);
			case 8:
				return LW(s);
			case 9:
				return SLL(s);
			case 10:
				return SRL(s);
			case 11:
				return SRA(s);
			case 12:
				return NOP(s);
			case 13:
				return ADD(s);
			case 14:
				return SUB(s);
			case 15:
				return MUL(s);
			case 16:
				return AND(s);
			case 17:
				return OR(s);
			case 18:
				return XOR(s);
			case 19:
				return NOR(s);
			case 20:
				return SLT(s);
			case 21:
				return ADDI(s);
			case 22:
				return ANDI(s);
			case 23:
				return ORI(s);
			case 24:
				return XORI(s);
			default:
				Integer x = Integer.parseInt(s);
					return x.toString();
			}}		else{
				Integer x = Integer.parseInt(s,2);
				return x.toString();
			}
	}
		
		private String NOP(String s) {
			// TODO Auto-generated method stub
			return "NOP";
		}


		private String SRA(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R2 = "R"+Integer.parseInt(s.substring(16,21), 2);
			String R3 = "R"+Integer.parseInt(s.substring(21,26), 2);
			return "SRA"+" "+R2+", "+R1+", "+"#"+Integer.parseInt(s.substring(21,26), 2);
		}


		private String SRL(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R2 = "R"+Integer.parseInt(s.substring(16,21), 2);
			String R3 = "R"+Integer.parseInt(s.substring(21,26), 2);
			
			return "SRL"+" "+R2+", "+R1+", "+"#"+Integer.parseInt(s.substring(21,26), 2);
		}


		private String ADD(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R3 = "R"+Integer.parseInt(s.substring(16,21), 2);
			
			return "ADD"+" "+R3+", "+R1+", "+R2;
		}
		
		private String J(String s) {
			// TODO Auto-generated method stub
			String shiftedString = s.substring(6,32)+"0"+"0";
			return "J "+"#"+Integer.parseInt(shiftedString, 2);
//			else
//				return "J "+"#"+-1*Integer.parseInt(shiftedString, 2);
		}

		private String SUB(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R3 = "R"+Integer.parseInt(s.substring(16,21), 2);
			
			return "SUB"+" "+R3+", "+R1+", "+R2;
		}
		
		private String JR(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			return "JR "+R1;
		}
		
		private String ADDI(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			return "ADDI"+" "+R2+", "+R1+", "+"#"+Integer.parseInt(s.substring(16,32), 2);
		}
		
		private String BEQ(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String shiftedString = s.substring(16,32)+"0"+"0";
			if(shiftedString.charAt(0) == '0')
			return "BEQ"+" "+R1+", "+R2+", "+"#"+Integer.parseInt(shiftedString, 2);
			else{
				char[] dataChar=shiftedString.toCharArray();
				for(int i=0;i<shiftedString.length();i++)
					dataChar[i] = (dataChar[i]=='0') ? '1':'0';
				shiftedString = new String(dataChar);
				int data = Integer.parseInt(shiftedString,2);
				data = -1 * (data+1);
				return "BEQ"+" "+R1+", "+R2+", "+"#"+data;
			}
		}


		private String MUL(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R3 = "R"+Integer.parseInt(s.substring(16,21), 2);
			
			return "MUL"+" "+R3+", "+R1+", "+R2;
		}

		private String BLTZ(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String shiftedString = s.substring(16,32)+"0"+"0";
			if(shiftedString.charAt(0) == '0')
			return "BLTZ"+" "+R1+", "+"#"+Integer.parseInt(shiftedString, 2);
			else{char[] dataChar=shiftedString.toCharArray();
				for(int i=0;i<shiftedString.length();i++)
				dataChar[i] = (dataChar[i]=='0') ? '1':'0';
			shiftedString = new String(dataChar);
			int data = Integer.parseInt(shiftedString,2);
			data = -1 * (data+1);
			return "BLTZ"+" "+R1+", "+"#"+data;
			}
		}

		private String AND(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R3 = "R"+Integer.parseInt(s.substring(16,21), 2);
			
			return "AND"+" "+R3+", "+R1+", "+R2;
		}
		
		private String BGTZ(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String shiftedString = s.substring(16,32)+"0"+"0";
			if(shiftedString.charAt(0) == '0')
			return "BGTZ"+" "+R1+", "+"#"+Integer.parseInt(shiftedString, 2);
			else {
				char[] dataChar=shiftedString.toCharArray();
			for(int i=0;i<shiftedString.length();i++)
				dataChar[i] = (dataChar[i]=='0') ? '1':'0';
			shiftedString = new String(dataChar);
			int data = Integer.parseInt(shiftedString,2);
			data = -1 * (data+1);
			return "BGTZ"+" "+R1+", "+"#"+data;
			}
		}


		private String OR(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R3 = "R"+Integer.parseInt(s.substring(16,21), 2);
			
			return "OR"+" "+R3+", "+R1+", "+R2;
		}
		
		
		private String LW(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String shiftedString=s.substring(16,32);
			if(shiftedString.charAt(0) == '0')
			return "LW"+" "+R2+", "+Integer.parseInt(s.substring(16,32), 2)+"("+R1+")";
			else {
				char[] dataChar=shiftedString.toCharArray();
				for(int i=0;i<shiftedString.length();i++)
					dataChar[i] = (dataChar[i]=='0') ? '1':'0';
				shiftedString = new String(dataChar);
				int data = Integer.parseInt(shiftedString,2);
				data = -1 * (data+1);
				return "LW"+" "+R2+", "+data+"("+R1+")";
			}
		}

		private String SW(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String shiftedString=s.substring(16,32);
			if(shiftedString.charAt(0) == '0')
			return "SW"+" "+R2+", "+Integer.parseInt(s.substring(16,32), 2)+"("+R1+")";
			else {
				char[] dataChar=shiftedString.toCharArray();
				for(int i=0;i<shiftedString.length();i++)
					dataChar[i] = (dataChar[i]=='0') ? '1':'0';
				shiftedString = new String(dataChar);
				int data = Integer.parseInt(shiftedString,2);
				data = -1 * (data+1);
				return "SW"+" "+R2+", "+data+"("+R1+")";
			}
			
		}

		
		private String XOR(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R3 = "R"+Integer.parseInt(s.substring(16,21), 2);
			
			return "XOR"+" "+R3+", "+R1+", "+R2;
		}


		private String SLT(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R3 = "R"+Integer.parseInt(s.substring(16,21), 2);
			
			return "SLT"+" "+R3+", "+R1+", "+R2;
		}
		
		private String NOR(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R3 = "R"+Integer.parseInt(s.substring(16,21), 2);
			
			return "NOR"+" "+R3+", "+R1+", "+R2;
		}


		private String ANDI(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			return "ANDI"+" "+R2+", "+R1+", "+"#"+Integer.parseInt(s.substring(16,32), 2);
		}


		private String ORI(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			return "ORI"+" "+R2+", "+R1+", "+"#"+Integer.parseInt(s.substring(16,32), 2);
		}


		private String XORI(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(6,11), 2);
			String R2 = "R"+Integer.parseInt(s.substring(11,16), 2);
			
			return "XORI"+" "+R2+", "+R1+", "+"#"+Integer.parseInt(s.substring(16,32), 2);
		}

		private String SLL(String s) {
			// TODO Auto-generated method stub
			String R1 = "R"+Integer.parseInt(s.substring(11,16), 2);
			String R2 = "R"+Integer.parseInt(s.substring(16,21), 2);
			String R3 = "R"+Integer.parseInt(s.substring(21,26), 2);
			return "SLL"+" "+R2+", "+R1+", "+"#"+Integer.parseInt(s.substring(21,26), 2);
		}

		private String BREAK(String s) {
			// TODO Auto-generated method stub
			return "BREAK";
		}

		/**
		 * @param args
		 */
		
	}

