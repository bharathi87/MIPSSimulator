import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;

public class MIPS_Processor {
	
	enum Operation {J,JR,BEQ,BLTZ,BGTZ,BREAK,SW,LW,SLL,SRL,SRA,NOP,
		ADD,SUB,MUL,AND,OR,XOR,NOR,SLT,ADDI,ANDI,ORI,XORI};
	int last_instr_addr,last_data_addr,instr_ptr,cur_instr_ptr;
	HashMap<String,Operation> Op;
	HashMap<String,Integer> Registers;
	HashMap<Integer,Integer> Memory;
	ArrayList<String> Instructions;	
	LinkedList<String> pre_issue,pre_alu1,pre_alu2,pre_mem,post_alu2,post_mem;
	LinkedList<String> pre_issue_new,pre_alu1_new,pre_alu2_new,pre_mem_new,post_alu2_new,
		post_mem_new;
	boolean branch_stall;
	String waiting_instr;
	String executed_instr;
	int reg_mem, reg_mem_new;
	int reg_alu, reg_alu_new;
	int mem_addr, mem_addr_new;
	MIPS_Processor() {
		Op=new HashMap<String,Operation>();
		Registers=new HashMap<String,Integer>();
		Memory=new HashMap<Integer,Integer>();
		Instructions=new ArrayList<String>();
		instr_ptr=256;
		pre_issue = new LinkedList<String>();
		pre_alu1 = new LinkedList<String>();
		pre_alu2 = new LinkedList<String>();
		pre_mem = new LinkedList<String>();
		post_alu2 = new LinkedList<String>();
		post_mem = new LinkedList<String>();
		branch_stall=false;
		//Initialize registers and operation hashmap
		initialize_reg();
	}

	void execute_instr(ArrayList<String> a) throws IOException {
		last_data_addr=256+(a.size()-1)*4;
		setmem_instr(a);
		execute();
	}
	
	private void execute() throws IOException  {
		int cycle=1;
		boolean buffers_empty = false;
		last_instr_addr=256+(Instructions.size()-1)*4;
		FileWriter fstream = new FileWriter("simulation.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		
		while (instr_ptr<=last_instr_addr) {
			buffers_empty = false;
			initialize_new_buffers();
			fetch();
			issue();
			alu1();
			alu2();
			mem();
			wb();
			reset_buffers();
			print_to_file(cycle,out);
			cycle++;
			if(pre_issue.size()==0 && 
				pre_alu1.size()==0 && 
				pre_alu2.size()==0 &&
				pre_mem.size()==0 &&
				post_alu2.size()==0 &&
				post_mem.size()==0) {
				buffers_empty = true;
			}
		}
		out.close();
		fstream.close();
	}
	
	private void issue() {
		boolean alu1_added = false, alu2_added = false;
		ListIterator<String> it=pre_issue.listIterator();
		int pre_alu1_capacity = 2-pre_alu1.size();
		int pre_alu2_capacity = 2-pre_alu2.size();
		while((pre_alu1_new.size()<pre_alu1_capacity || pre_alu2_new.size()<pre_alu2_capacity)
				&& it.hasNext() && (!alu1_added || !alu2_added)) {
			String instr =it.next();
			String[] str = instr.trim().split("[\\s,()#]+");
			String Opcode = str[0];
			Operation oper=Op.get(Opcode);
			
			//Check for RAW, WAR and WAW hazards
			if(check_raw(post_mem,instr,post_mem.size()-1) || 
					check_raw(pre_mem,instr,pre_mem.size()-1) || 
					check_raw(post_alu2,instr,post_alu2.size()-1) ||
					check_raw(pre_alu1,instr,pre_alu1.size()-1) ||
					check_raw(pre_alu2,instr,pre_alu2.size()-1) ||
					check_raw(pre_issue,instr,it.nextIndex()-2) || 
					
					check_waw(post_mem,instr,post_mem.size()-1) || 
					check_waw(pre_mem,instr,pre_mem.size()-1) || 
					check_waw(post_alu2,instr,post_alu2.size()-1) ||
					check_waw(pre_alu1,instr,pre_alu1.size()-1) ||
					check_waw(pre_alu2,instr,pre_alu2.size()-1) ||
					check_waw(pre_issue,instr,it.nextIndex()-2) ||
					
					check_war(pre_issue,instr,it.nextIndex()-2)){
				continue;
			}
			//IF load/store push to alu1
			if((oper == Operation.LW || oper == Operation.SW)) {
				if (!is_store(pre_issue,it.nextIndex()-2)
						&& !alu1_added && !check_waw(pre_alu2_new,instr,pre_alu2_new.size()-1) && 
						!check_war(pre_alu2_new,instr,pre_alu2_new.size()-1) && !check_raw(pre_alu2_new,instr,pre_alu2_new.size()-1)) {
					it.remove();
					pre_alu1_new.addLast(instr);
					alu1_added=true;
				}
			}
			else { //Else push to alu2
				if (!alu2_added && !check_waw(pre_alu1_new,instr,pre_alu1_new.size()-1) && 
						!check_war(pre_alu1_new,instr,pre_alu1_new.size()-1) && !check_raw(pre_alu1_new,instr,pre_alu1_new.size()-1)){
					it.remove();
					pre_alu2_new.addLast(instr);
					alu2_added=true;
				}
			}
		}
	}
	private void alu1() {
		if (pre_alu1.size()==0) return;
		
		String instr = pre_alu1.removeFirst();
		String[] str = instr.trim().split("[\\s,()#]+");
		pre_mem_new.addLast(instr);
		mem_addr_new = (Integer.parseInt(str[2]) + Registers.get(str[3]));	
	}
	private void alu2() {
		if (pre_alu2.size()==0) return;
		String instr = pre_alu2.removeFirst();
		post_alu2_new.addLast(instr);
		String[] str = instr.trim().split("[\\s,()#]+");
		String Opcode = str[0];
		switch(Op.get(Opcode)) {
		case SLL:
			reg_alu_new = Registers.get(str[2]) << Integer.parseInt(str[3]);
			break;
		case SRL:
			reg_alu_new = Registers.get(str[2]) >>> Integer.parseInt(str[3]);
			break;
		case NOP:
			break;
		case ADD:
			reg_alu_new = Registers.get(str[2]) + Registers.get(str[3]);
			break;
		case SUB:
			reg_alu_new = Registers.get(str[2]) - Registers.get(str[3]);
			break;
		case MUL:
			reg_alu_new = Registers.get(str[2]) * Registers.get(str[3]);
			break;
		case AND:
			reg_alu_new = Registers.get(str[2]) & Registers.get(str[3]);
			break;
		case OR:
			reg_alu_new = Registers.get(str[2]) | Registers.get(str[3]);
			break;
		case XOR:
			reg_alu_new = Registers.get(str[2]) ^ Registers.get(str[3]);
			break;
		case NOR:
			reg_alu_new = ~(Registers.get(str[2]) ^ Registers.get(str[3]));
			break;
		case SLT:
			if (Registers.get(str[2]) < Registers.get(str[3])) {
				reg_alu_new = 1;
			} else {
				reg_alu_new = 0;
			}
			break;
		case ADDI:
			reg_alu_new = Registers.get(str[2]) + Integer.parseInt(str[3]);
			break;
		case ANDI:
			reg_alu_new = Registers.get(str[2]) & Integer.parseInt(str[3]);
			break;
		case ORI:
			reg_alu_new = Registers.get(str[2]) | Integer.parseInt(str[3]);
			break;
		case XORI:
			reg_alu_new = Registers.get(str[2]) ^ Integer.parseInt(str[3]);
			break;
		case SRA:
			reg_alu_new = Registers.get(str[2]) >> Integer.parseInt(str[3]);
			break;
		default:
			break;
		}
	}
	private void mem() {
		if(pre_mem.size()==0)  return;
		
		String instr = pre_mem.removeFirst();
		String[] str = instr.trim().split("[\\s,()#]+");
		String Opcode = str[0];
		if(Op.get(Opcode) == Operation.SW) {
			Memory.put(mem_addr,
				Registers.get(str[1]));
		}
		else if(Op.get(Opcode) == Operation.LW) {
 			reg_mem_new = Memory.get(mem_addr);
			post_mem_new.addLast(instr);
		}
	}
	private void wb() {
		if(post_mem.size()!=0) {
			String instr = post_mem.removeFirst();
			String[] str = instr.trim().split("[\\s,()#]+");
			String Opcode = str[0];
			if(Op.get(Opcode) == Operation.LW) {
				Registers.put(str[1], reg_mem);
			}
		}
		if(post_alu2.size()!=0) {
			String instr = post_alu2.removeFirst();
			String[] str = instr.trim().split("[\\s,()#]+");
			//post_alu2.removeFirst();
			Registers.put(str[1], reg_alu);
		}
		
	}
	private void fetch() {
		executed_instr=null;
		if (branch_stall) {// If branch stall, check if stall exists still
			String instr=waiting_instr;
			if(check_raw(post_mem,instr,post_mem.size()-1) || 
					check_raw(pre_mem,instr,pre_mem.size()-1) ||
					check_raw(post_alu2,instr,post_alu2.size()-1) ||
					check_raw(pre_alu1,instr,pre_alu1.size()-1) ||
					check_raw(pre_alu2,instr,pre_alu2.size()-1) ||
					check_raw(pre_issue,instr,pre_issue.size()-1) ||
					check_raw(pre_issue,instr,pre_issue.size()-1) ||
					check_raw(pre_issue_new,instr,pre_issue_new.size()-1) ||
					check_raw(pre_issue_new,instr,pre_issue_new.size()-1)) {
				return;
			}
			else {
				branch_stall=false;
				waiting_instr=null;
			}
		}
		int pre_issue_capacity=4-pre_issue.size();
		for(int i=0;i<2 && instr_ptr<=last_instr_addr;i++) {
			if(pre_issue_new.size() < pre_issue_capacity) {
				
				String instr=Instructions.get((instr_ptr-256)/4);
				String[] str = instr.trim().split("[\\s,()#]+");
				String Opcode = str[0];
				Operation oper=Op.get(Opcode);
				if(oper == Operation.BEQ || oper == Operation.BGTZ || oper == Operation.BLTZ
						|| oper == Operation.JR) { //If conditional branch, check for read operand hazards
					if(check_raw(post_mem,instr,post_mem.size()-1) || 
						check_raw(pre_mem,instr,pre_mem.size()-1) ||
						check_raw(post_alu2,instr,post_alu2.size()-1) ||
						check_raw(pre_alu1,instr,pre_alu1.size()-1) ||
						check_raw(pre_alu2,instr,pre_alu2.size()-1) ||
						check_raw(pre_issue,instr,pre_issue.size()-1) ||
						check_raw(pre_issue,instr,pre_issue.size()-1) ||
						check_raw(pre_issue_new,instr,pre_issue_new.size()-1) ||
						check_raw(pre_issue_new,instr,pre_issue_new.size()-1)
						) {

						branch_stall=true;
						waiting_instr=instr;
						break;
					}
					instr_ptr+=4;
					executed_instr=instr;
					switch(oper) {
					case BEQ:
						if (Registers.get(str[1]) == Registers.get(str[2])) {
							instr_ptr = instr_ptr + Integer.parseInt(str[3]);
						}
						break;
					case BGTZ:
						if (Registers.get(str[1]) > 0) {
							instr_ptr = instr_ptr + Integer.parseInt(str[2]);
						}
						break;
					case BLTZ:
						if (Registers.get(str[1]) < 0) {
							instr_ptr = instr_ptr + Integer.parseInt(str[2]);
						}
						break;
					case JR:
						instr_ptr = Registers.get(str[1]);
						break;
					}
					break;		
				}
				else if (oper == Operation.J) {
					executed_instr=instr;
					instr_ptr+=4;
					instr_ptr=(instr_ptr & 15<<28) | (Integer.parseInt(str[1])&(-1>>>4));
					break;
				}
		
				else if (oper == Operation.NOP) {
					executed_instr=instr;
					instr_ptr+=4;
					continue;
				}
				else if (oper == Operation.BREAK) {
					executed_instr=instr;
					instr_ptr+=4;
					break;
				}
				instr_ptr+=4;
				pre_issue_new.addLast(instr);
			}
			else {
				break;
			}
		}
	}
	private void print_to_file(int cycle,BufferedWriter out) throws IOException {
		int data_addr=last_instr_addr+4;
		out.write("--------------------\n");
		out.write("Cycle:"+cycle+"\n");
		out.write("\n");
		out.write("IF Unit:\n");
		if(waiting_instr!=null)
			out.write("\t"+"Waiting Instruction: ["+waiting_instr+"]\n");
		else
			out.write("\t"+"Waiting Instruction:\n");
		if(executed_instr!=null)
			out.write("\t"+"Executed Instruction: ["+executed_instr+"]\n");
		else
			out.write("\t"+"Executed Instruction:\n");		
		
		out.write("Pre-Issue Queue:\n");
		print_buffer(out,pre_issue,4);
		out.write("Pre-ALU1 Queue:\n");
		print_buffer(out,pre_alu1,2);
		if(pre_mem.size()>0)
			out.write("Pre-MEM Queue: ["+pre_mem.get(0)+"]\n");
		else
			out.write("Pre-MEM Queue:\n");
		if(post_mem.size()>0)
			out.write("Post-MEM Queue: ["+post_mem.get(0)+"]\n");
		else
			out.write("Post-MEM Queue:\n");
		
		out.write("Pre-ALU2 Queue:\n");
		print_buffer(out,pre_alu2,2);
		if(post_alu2.size()>0)
			out.write("Post-ALU2 Queue: ["+post_alu2.get(0)+"]\n");
		else
			out.write("Post-ALU2 Queue:\n");
		out.write("\n");
		out.write("Registers");
		for(int i=0;i<32;i++) {
			if(i==0 || i==8)  {
				out.write("\n");
				out.write("R0"+i+":");
			}
			
			else if (i==16 || i==24) {
				out.write("\n");
				out.write("R"+i+":");
			}
			
			if(i==7 || i==15 || i==23 || i==31) 
				out.write("\t"+Registers.get("R"+i));
			else
				out.write("\t"+Registers.get("R"+i));
		}
		out.write("\n");
		out.write("\n");
		out.write("Data");
		
		while (data_addr<=last_data_addr) {
			if((data_addr-(last_instr_addr+4))%32==0) {
				out.write("\n");
				out.write(data_addr+":");
			}
			out.write("\t"+Memory.get(data_addr));	
			data_addr+=4;
		}
			out.write("\n");
			
	}
	
	private void print_buffer(BufferedWriter out,LinkedList<String> buf,int size) throws IOException {
		int i=0;
		for(i=0;i<buf.size();i++) {
			out.write("\t"+"Entry "+i+": ["+buf.get(i)+"]\n");
		}
		for(;i<size;i++) {
			out.write("\t"+"Entry "+i+":\n");
		}
	}
	
	private boolean is_store(LinkedList<String> buf,int index) {

		String[] str1=null;
		//String Opcode=null;
		//ArrayList<String> regs = new ArrayList<String>();
		ListIterator<String> i = buf.listIterator();
		while(i.hasNext() && i.nextIndex()<=index){
			str1 = i.next().trim().split("[\\s,()#]+");
			if("SW".equals(str1[0]))
				return true;
		}
				
		return false;
	}
	private boolean check_raw(LinkedList<String> buf,String instr, int index) {
		String[] str = instr.trim().split("[\\s,()#]+");
		String[] str1=null;
		String Opcode=str[0];
		String Opcode1=null;
		ArrayList<String> regs = new ArrayList<String>();
		ListIterator<String> i = buf.listIterator();
		
		if(index==-1)
			return false;
		
		while(i.hasNext() && i.nextIndex()<=index){
			str1 = i.next().trim().split("[\\s,()#]+");
			Opcode1 = str1[0];
			switch (Op.get(Opcode1)) {
			case J:
			case JR:
			case BEQ:
			case BGTZ:
			case BLTZ:
			case BREAK:
			case NOP:
			case SW:
				break;
			case ADDI:
			case ANDI:
			case ORI:
			case SLL:
			case SRL:
			case XORI:
			case LW:
			case ADD:
			case SUB:
			case MUL:
			case AND:
			case OR:
			case XOR:
			case NOR:
			case SLT:
			case SRA:
				regs.add(str1[1]);
				break;
			default:				
				break;
			}
		}

		switch (Op.get(Opcode)) {
		case J:
			break;
		case JR:
			if(regs.contains(str[1]))
				return true;		
			break;
		case BEQ:
		case BGTZ:
		case BLTZ:
			if(regs.contains(str[1]) || regs.contains(str[2]))
				return true;
			break;
		case BREAK:
			break;
		case ADDI:
		case ANDI:
		case ORI:
		case SLL:
		case SRL:
		case SRA:
		case XORI:
			if(regs.contains(str[2]))
				return true;
			break;
		case SW:
			if(regs.contains(str[1])||regs.contains(str[3]))
				return true;
			
			break;
		case LW:
			if(regs.contains(str[3]))
				return true;
			break;
		case NOP:
			break;
		case ADD:
		case SUB:
		case MUL:
		case AND:
		case OR:
		case XOR:
		case NOR:
		case SLT:
			if(regs.contains(str[2])||regs.contains(str[3]))
				return true;
			break;
		default:				
			break;
		}

		return false;
	}

	private boolean check_waw(LinkedList<String> buf,String instr, int index) {
		String[] str = instr.trim().split("[\\s,()#]+");
		String[] str1=null;
		String Opcode=str[0];
		String Opcode1=null;
		ArrayList<String> regs = new ArrayList<String>();
		ListIterator<String> i = buf.listIterator();
		while(i.hasNext() && i.nextIndex()<=index){
			str1 = i.next().trim().split("[\\s,()#]+");
			Opcode1 = str1[0];
			switch (Op.get(Opcode1)) {
			case J:
				break;
			case JR:
				break;
			case BEQ:
				break;
			case BGTZ:
				break;
			case BLTZ:
				break;
			case BREAK:
				break;
			case SW:
				break;
			case NOP:
				break;
			case ADDI:
			case ANDI:
			case ORI:
			case SLL:
			case SRL:
			case XORI:
			case LW:
			case ADD:
			case SUB:
			case MUL:
			case AND:
			case OR:
			case XOR:
			case NOR:
			case SLT:
			case SRA:
				regs.add(str1[1]);
				break;
			default:				
				break;
			}
		}
		
		if(regs.contains(str[1]) && Op.get(Opcode)!=Operation.SW) {
			return true;
		}
		return false;
	}
	
	private boolean check_war(LinkedList<String> buf,String instr, int index) {

		String[] str = instr.trim().split("[\\s,()#]+");
		String[] str1=null;
		String Opcode=str[0];
		String Opcode1=null;
		ArrayList<String> regs = new ArrayList<String>();
		ListIterator<String> i = buf.listIterator();
		
		if(index==-1)
			return false;
		
		while(i.hasNext() && i.nextIndex()<=index){
			str1 = i.next().trim().split("[\\s,()#]+");
			Opcode1 = str1[0];
			switch (Op.get(Opcode1)) {
			case J:
			case JR:
			case BEQ:
			case BGTZ:
			case BLTZ:
			case BREAK:
				break;
			case ADDI:
			case ANDI:
			case ORI:
			case SLL:
			case SRL:
			case SRA:
			case XORI:
				regs.add(str1[2]);
				break;
			case SW:
				regs.add(str1[1]);
				regs.add(str1[3]);
				break;
			case LW:
				regs.add(str1[3]);
				break;
			case NOP:
				break;
			case ADD:
			case SUB:
			case MUL:
			case AND:
			case OR:
			case XOR:
			case NOR:
			case SLT:
				regs.add(str1[2]);
				regs.add(str1[3]);
				break;
			default:				
				break;
			}
		}
		
		if(regs.contains(str[1]) && Op.get(Opcode)!=Operation.SW){
			return true;
		}
		
		return false;
	}

	private void initialize_new_buffers() {
		pre_issue_new = new LinkedList<String>();
		pre_alu1_new = new LinkedList<String>();
		pre_alu2_new = new LinkedList<String>();
		pre_mem_new = new LinkedList<String>();
		post_alu2_new = new LinkedList<String>();
		post_mem_new = new LinkedList<String>();
	}
	
	private void reset_buffers() {
		while(!pre_issue_new.isEmpty()) {
			String str=pre_issue_new.removeFirst();
			pre_issue.addLast(str);
		}
		while(!pre_alu1_new.isEmpty()) {
			String str=pre_alu1_new.removeFirst();
			pre_alu1.addLast(str);
		}
		while(!pre_alu2_new.isEmpty()) {
			String str=pre_alu2_new.removeFirst();
			pre_alu2.addLast(str);
		}
		while(!post_mem_new.isEmpty()) {
			String str=post_mem_new.removeFirst();
			post_mem.addLast(str);
		}
		while(!pre_mem_new.isEmpty()) {
			String str=pre_mem_new.removeFirst();
			pre_mem.addLast(str);
		}
		while(!post_alu2_new.isEmpty()) {
			String str=post_alu2_new.removeFirst();
			post_alu2.addLast(str);
		}
		reg_mem = reg_mem_new;
		reg_alu = reg_alu_new;
		mem_addr = mem_addr_new;
	}
//	private boolean check_data_hazard(LinkedList<String> buf,String instr,int index) {
//		return false;
//		
//	}
	private void setmem_instr(ArrayList<String> a) {
		int cnt=256;
		//int instr=0;
		boolean flag=false;
		int i=0;
		//System.out.println(a);

		for(String s: a) {
			if("BREAK".equals(s)) {
				flag=true;
				cnt+=4;
				//instr=cnt;
				Instructions.add(a.get(i++));
				continue;
			}
			if(flag) {	
				Memory.put(cnt,Integer.parseInt(s));
			}
			else {
				Instructions.add(a.get(i++));
			}
			cnt+=4;
		}	
	}	
	private void initialize_reg() {
		Op.put("J", Operation.J);
		Op.put("JR", Operation.JR);
		Op.put("BEQ", Operation.BEQ);
		Op.put("BGTZ", Operation.BGTZ);
		Op.put("BLTZ", Operation.BLTZ);
		Op.put("BREAK", Operation.BREAK);
		Op.put("SW", Operation.SW);
		Op.put("LW", Operation.LW);
		Op.put("SLL", Operation.SLL);
		Op.put("SRL", Operation.SRL);
		Op.put("SRA", Operation.SRA);
		Op.put("ADD", Operation.ADD);
		Op.put("SUB", Operation.SUB);
		Op.put("MUL", Operation.MUL);
		Op.put("AND", Operation.AND);
		Op.put("OR", Operation.OR);
		Op.put("XOR", Operation.XOR);
		Op.put("NOR", Operation.NOR);
		Op.put("SLT", Operation.SLT);
		Op.put("ADDI", Operation.ADDI);
		Op.put("ANDI", Operation.ANDI);
		Op.put("ORI", Operation.ORI);
		Op.put("XORI", Operation.XORI);
		for(int i=0;i<32;i++) {
			Registers.put("R"+i, 0);
		}
	}
	
}
	
	
