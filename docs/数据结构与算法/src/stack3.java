public class stack3 {

    public static void main(String[] args) {
        System.out.println(factor(6));
        System.out.println(factor2(6));
        System.out.println(factor3(6));
    }
    public static int factor(int num) {
        if(num == 1 || num == 0) {
            return 1;
        }

        return num * factor(num - 1);
    }
    
    public static int factor2(int num) {
        int result = 1;
        for(int i = 2; i <= num; i++) {
            result *= i;
        }
        return result;
    }

    public static int factor3(int num) {
        
        Link top = null;
        int i = num;
        while(i > 0) {
            top = new Link(i, top);
            i--;
        }
        int result = 1;
        while(top!=null) {
            result *= top.value;
            top = top.next;
        }
        return result;
    }


    static class Link {
        int value;
        Link next;
        Link(int value, Link next){
            this.value = value;
            this.next = next;
        }
    }
}