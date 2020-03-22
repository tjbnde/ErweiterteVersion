public class Balancer {

    public int returnRandomNumber() {
        double random =  Math.random();
        if (random < 0.5) {
            return 0;
        } else {
            return 1;
        }
    }


}
