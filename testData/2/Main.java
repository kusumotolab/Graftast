public class Main {

	public static void main(String args[]) {
		int a = 2;
		int b = 3;
		
		int ans = Calc.multi(a, b);
		
		System.out.println("answer : " + ans);
	}

}


class Calc {

	public static int add(int num1, int num2) {
		return num1 + num2;
	}
	
	public static int multi(int num1, int num2) {
		return num1 * num2;
	}

}
