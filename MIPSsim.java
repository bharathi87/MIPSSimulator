
	import java.io.IOException;
	import java.util.ArrayList;
	
	
	public class MIPSsim {

		public static void main(String[] args) throws IOException {
			// TODO Auto-generated method stub
			Assembler a = new Assembler();
			ArrayList<String> list= a.FillInArrayList(args[0]);
			a.writeOutToFile(args[0], list, "disassembly.txt");
			MIPS_Processor k=new MIPS_Processor();
			k.execute_instr(list);
		}


	}
