package CAssemblerForMem;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 処理の本体（memory.txtの方）
 * @author fujimotoakira
 *
 */
public class Processor3 {

	final private List<String> inputBuffer;
	private List<Label> labelList = new LinkedList<Label>();

	public Processor3(List<String> inputBuffer) {
		this.inputBuffer = inputBuffer;
		registerLabel(); //FPGAボードで使用する割り当て機器へのラベル
	}

	/**
	 * 処理メソッド
	 * @return 最終的なコード
	 * @throws SyntaxException
	 */
	public List<String> process() throws SyntaxException {
		List<String> codeInfo = phase1();
		List<String> code = phase2(codeInfo);
		return code;
	}

	/**
	 * 1周のチェック
	 * コメント分離，ラベル識別
	 * @return 命令語とオペランドのリスト
	 * @throws SyntaxException
	 */
	public List<String> phase1() throws SyntaxException {
		int counter = 0;
		int constantCounter = 0;
		List<String> code = new LinkedList<String>();
		for(int i = 0 ; i < inputBuffer.size() ; i += 1) {
			String string = inputBuffer.get(i);
			string = string.split("#")[0];	//コメントを削除
			Pattern p = Pattern.compile("^\t{2}");
	        Matcher m = p.matcher(string);
			if(string.equals("") || string.equals("\t") || m.find()) continue;	//空行を読み飛ばす（tabの連続も含める）
			String[] kari = string.split("\\t",-1);
			if(!kari[0].equals("")) {
				//ラベルが入ったときリストに追加
				if(searchLabel(kari[0]) != -1 )
					throw new SyntaxException("Duplicated label.", i);
				labelList.add(new Label(kari[0], counter));
			}
			string = kari[1];
			String splitStr[] = string.split(","); //","で命令語とオペランドを分割
			if(splitStr.length == 2) {
				switch(splitStr[0]) {
				case "SETIXH":
				case "SETIXL":
				case "LDIA":
				case "LDIB":
				case "STDI":
					code.add(splitStr[0]);
					code.add(splitStr[1]);
					counter += 2;
					break;
				case "JP":
				case "JPC":
				case "JPZ":
					code.add(splitStr[0]);
					code.add(splitStr[1]);
					code.add(splitStr[1]);
					counter += 3;
					break;
				case "DC":
					code.add(splitStr[0]);
					code.add(splitStr[1]);
					labelList.get(labelList.size()-1).changeLine(0x8000+constantCounter++); //DCの領域を確保
					counter += 1;
					break;
				case "SETIX":
					code.add("SETIXH");
					code.add(splitStr[1]);
					code.add("SETIXL");
					code.add(splitStr[1]);
					counter += 4;
					break;
				default:
					throw new SyntaxException(splitStr[0] + " is not found or wrong operand.", i);
				}
			}else if(splitStr.length == 3) {
				switch(splitStr[0]) {
				case "JP":
				case "JPC":
				case "JPZ":
					code.add(splitStr[0]);
					code.add(splitStr[1]);
					code.add(splitStr[2]);
					counter += 3;
					break;
				default:
					throw new SyntaxException(splitStr[0] + " is not found or wrong operand.", i);
				}
			}else {
				switch(splitStr[0]) {
				case "LDDA":
				case "LDDB":
				case "STDA":
				case "STDB":
				case "ADDA":
				case "SUBA":
				case "ANDA":
				case "ORA":
				case "NOTA":
				case "INCA":
				case "DECA":
				case "ADDB":
				case "SUBB":
				case "ANDB":
				case "ORB":
				case "NOTB":
				case "INCB":
				case "DECB":
				case "CMP":
				case "NOP":
					code.add(splitStr[0]);
					counter += 1;
					break;
				default:
					throw new SyntaxException(splitStr[0] + " is not found or wrong operand.", i);
				}
			}
		}
		return code;
	}

	/**
	 * 2周目．
	 * 機械語への変換．ラベルの対応づけ
	 * @param code phase1で返ったリスト
	 * @return 最終的なコード
	 * @throws SyntaxException
	 */
	public List<String> phase2(List<String> code) throws SyntaxException {
		List<String> byteCode = new LinkedList<String>();
		boolean greatAddress = true;
		boolean setixh = false;
		boolean setixl = false;
		int label = 0;

		for(int i = 0 ; i < code.size() ; i++) {
			String line = String.format("%04x    ", i);
			String codeInfo = code.get(i);
			switch(codeInfo) {
			case "SETIXH":
				line += "d0";
				setixh = true;
				break;
			case "SETIXL":
				line += "d1";
				setixl = true;
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
				break;
			case "JPC":
				line += "40";
				break;
			case "JPZ":
				line += "50";
			case "SETIX":
				break;
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
			case "DC":
				line = String.format("%04x    ", 0x8000+label++);
				i += 1;
				String string = code.get(i);
				if(string.length() == 1)
					line += "0" + string;
				else if(string.length() == 2)
					line += string;
				else
					throw new SyntaxException("DC operand must be single or double-digit.", -1);
				break;
			default:
				//アドレスに変換（ラベルの時）
				int address;
				if((address = searchLabel(codeInfo)) == -1) {
					//一致するラベルがなかった時
					Pattern p = Pattern.compile("[0-9|A-F|a-f]{1,2}");
			        Matcher m = p.matcher(codeInfo);
					if(m.matches()) {
						//数値のみ　アドレスとみなす
						if (codeInfo.length() == 1)
							line += "0" + codeInfo;
						else if(codeInfo.length() == 2)
							line += codeInfo;
						else
							throw new SyntaxException("Address must be double-digit.", -1);
					}else {
						throw new SyntaxException("No valid label \""+codeInfo+"\"", -1);
					}
				}else {
					//ラベルを番地に書き換え
					String fourBit = String.format("%04x", address);
					if(setixh) {
						line += fourBit.substring(0,2);
						setixh = false;
					}else if(setixl) {
						line += fourBit.substring(2);
						setixl = false;
					}else if(greatAddress) {
						line += fourBit.substring(0,2);
						greatAddress = !greatAddress;
					}else {
						line += fourBit.substring(2);
						greatAddress = !greatAddress;
					}
					line += "    -- to " + codeInfo;	//ただのコメント（行き先）
				}
				byteCode.add(line);
				continue;
			}
			line += "    -- " + codeInfo;	//ただのコメント（命令）
			byteCode.add(line);
		}
		return byteCode;
	}

	/**
	 * ラベルに対応する番地を返す
	 * @param codeInfo ラベル名
	 * @return 対応する番地
	 */
	private int searchLabel(String codeInfo) {
		for(Label label: labelList) {
			if(label.getLabel().equals(codeInfo)) return label.getLine();
		}
		return -1;
	}
	
	/**
	 * FPGAボードのボタンなどへのメモリ割付領域用のラベルを設定
	 */
	private void registerLabel() {
		labelList.add(new Label("KEY_CD", 0xFFFB));
		labelList.add(new Label("DIP_A", 0xFFFC));
		labelList.add(new Label("DIP_B", 0xFFFD));
		labelList.add(new Label("SEG_AB", 0xFFFE));
		labelList.add(new Label("SEG_CD", 0xFFFF));
	}
}
