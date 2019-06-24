package CAssemblerForMem;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 処理の本体クラス．<br>
 * こちらは先に機械語に変換する．
 * よって，ラベルに命令語を描いても動作するが機械語コード"d0"とかを書くとバグる．<br>
 * 結果ボツになった
 * @author fujimotoakira
 *
 */
public class Processor2 {

	final private List<String> inputBuffer;
	private List<Label> labelList = new LinkedList<Label>();

	public Processor2(List<String> inputBuffer) {
		this.inputBuffer = inputBuffer;
	}

	public List<String> process() throws SyntaxException {
		List<CodeInfo> codeInfo = phase1();
		List<String> code = phase2(codeInfo);
		return code;
	}

	/**
	 * 1週目のチェック
	 * @throws SyntaxException
	 */
	public List<CodeInfo> phase1() throws SyntaxException {
		int counter = 0;
		int constantCounter = 0;
		List<CodeInfo> code = new LinkedList<CodeInfo>();
		for(int i = 0 ; i < inputBuffer.size() ; i += 1) {
			String string = inputBuffer.get(i);
			string = string.split("#")[0];	//コメントを削除
			Pattern p = Pattern.compile("^\t{2}");
	        Matcher m = p.matcher(string);
			if(string.equals("") || string.equals("\t") || m.find()) continue;	//空行を読み飛ばす（tabの連続も含める）
			String[] kari = string.split("\\t",-1);
			if(!kari[0].equals("")) {
				//ラベルが入ったときリストに追加
				labelList.add(new Label(kari[0], counter));
			}
			string = kari[1];
			String splitStr[] = string.split(",");
			String line = "";
			if(splitStr.length == 2) {
				boolean jpFlag = false;
				boolean dcFlag = false;
				switch(splitStr[0]) {
				case "SETIXH":
					line += "d0";
					break;
				case "SETIXL":
					line += "d1";
					break;
				case "LDIA":
					line += "d8";
					break;
				case "LDIB":
					line += "d9";
					break;
				case "STDI":
					line += "f8";
					break;
				case "JP":
					line += "60";
					jpFlag = true;
					break;
				case "JPC":
					line += "40";
					jpFlag = true;
					break;
				case "JPZ":
					line += "50";
					jpFlag = true;
					break;
				case "DC":
					line += splitStr[1];
					labelList.get(labelList.size()-1).changeLine(0x8000+constantCounter++);
					dcFlag = true;
					break;
				case "SETIX":
					code.add(new CodeInfo("d0", "SETIXH(SETIX)"));
					code.add(new CodeInfo(splitStr[1], ""));
					code.add(new CodeInfo("d1", "SETIXL(SETIX)"));
					code.add(new CodeInfo(splitStr[1], ""));
					continue;
				default:
					throw new SyntaxException(splitStr[0], i);
				}
				code.add(new CodeInfo(line, splitStr[0]));
				if(dcFlag) continue;
				code.add(new CodeInfo(splitStr[1], ""));
				counter += 2;
				if(jpFlag) {
					code.add(new CodeInfo(splitStr[1], ""));
					counter += 1;
				}
			}else if(splitStr.length == 3) {
				switch(splitStr[0]) {
				case "JP":
					line += "60";
					break;
				case "JPC":
					line += "40";
					break;
				case "JPZ":
					line += "50";
					break;
				default:
					throw new SyntaxException(splitStr[0], i);
				}
				code.add(new CodeInfo(line, splitStr[0]));
				code.add(new CodeInfo(splitStr[1], ""));
				code.add(new CodeInfo(splitStr[2], ""));
				counter += 3;
			}else {
				switch(splitStr[0]) {
				case "LDDA":
					line += "e0";
					break;
				case "LDDB":
					line += "e1";
					break;
				case "STDA":
					line += "f0";
					break;
				case "STDB":
					line += "f4";
					break;
				case "ADDA":
					line += "80";
					break;
				case "SUBA":
					line += "81";
					break;
				case "ANDA":
					line += "82";
					break;
				case "ORA":
					line += "83";
					break;
				case "NOTA":
					line += "84";
					break;
				case "INCA":
					line += "85";
					break;
				case "DECA":
					line += "86";
					break;
				case "ADDB":
					line += "90";
					break;
				case "SUBB":
					line += "91";
					break;
				case "ANDB":
					line += "92";
					break;
				case "ORB":
					line += "93";
					break;
				case "NOTB":
					line += "98";
					break;
				case "INCB":
					line += "99";
					break;
				case "DECB":
					line += "9a";
					break;
				case "CMP":
					line += "a1";
					break;
				case "NOP":
					line += "00";
					break;
				default:
					throw new SyntaxException(splitStr[0], i);
				}
				code.add(new CodeInfo(line, splitStr[0]));
				counter += 1;
			}
		}
		return code;
	}

	public List<String> phase2(List<CodeInfo> codeInfos) throws SyntaxException {
		List<String> code = new LinkedList<String>();
		boolean greatAddress = true;
		int label = 0;
		for(int i = 0 ; i < codeInfos.size() ; i++) {
			String line = String.format("%04x    ", i);
			CodeInfo codeInfo = codeInfos.get(i);
			if(codeInfo.getInstruction().equals("")) {
				int address;
				if(codeInfo.getByteCode().length() < 3) {
					line += codeInfo.getByteCode();
					line += "    --";
				}else if((address = searchLabel(codeInfo)) == -1) {
					throw new SyntaxException("No valid label \""+codeInfo.getByteCode()+"\"", i);
				}else {
					//ラベルを番地に書き換え
					String fourBit = String.format("%04x", address);
					if(greatAddress) {
						line += fourBit.substring(0,2);
					}else {
						line += fourBit.substring(2);
					}
					greatAddress = !greatAddress;
					line += "    --";
				}
			}else {
				if(codeInfo.getInstruction().equals("DC")) {
					line = String.format("%04x    ", 0x8000+label++);
				}
				line += codeInfo.getByteCode();
				line += "    -- ";
				line += codeInfo.getInstruction();
			}
			code.add(line);
		}
		return code;
	}

	public int searchLabel(CodeInfo codeInfo) {
		String byteCode = codeInfo.getByteCode();
		for(Label label: labelList) {
			if(label.getLabel().equals(byteCode)) return label.getLine();
		}
		return -1;
	}

}


class CodeInfo {
	final public String byteCode;
	final public String instruction;

	public CodeInfo(String b, String i) {
		this.byteCode = b;
		this.instruction = i;
	}

	public String getByteCode() {
		return byteCode;
	}

	public String getInstruction() {
		return instruction;
	}

}
