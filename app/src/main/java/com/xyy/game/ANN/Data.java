package com.xyy.game.ANN;

public class Data {
	
	public double[][] GetInputSet() {
        //return new double[][]{{1,1,0},{-0.8,-0.8,0},{0,1,-0.4},{0,1,0.4},{0,0,0}};
        return new double[][]{
                {1,1,0,-1,-1,-1,-1},
                {-0.5,-0.5,0,-1,-1,-1,-1},
                {0,-0.5,-0.4,-1,-1,-1,-1},
                {0,-0.5,0.4,-1,-1,-1,-1},
                {0,0,0,-1,-1,-1,-1},
                {1,-0.5,-0.4,-1,-1,-1,-1},
                {1,-0.5,0.4,-1,-1,-1,-1},
                {1,-0.5,0,-1,-1,-1,-1},

                {1,1,0,1,-1,-1,-1},
                {-0.5,-0.5,0,-1,1,-1,-1},
                {0,-0.5,-0.4,-1,-1,1,-1},
                {0,-0.5,0.4,-1,-1,-1,1},

                {1,-0.5,-0.4,-1,-1,1,-1},
                {1,-0.5,0.4,-1,-1,-1,1}

        };
	}

	public double[][] GetOutputSet() {
        //return new double[][]{{1,0,0,0},{0,1,0,0},{0,1,1,0},{0,1,0,1},{0,0,0.9,0.9}};
        return new double[][]{
                {1,0,0.9,0.9},
                {0,1,0.9,0.9},
                {0,1,1,0},
                {0,1,0,1},
                {0,0,0.9,0.9},
                {1,0,1,0},
                {1,0,0,1},
                {1,0,0.9,0.9},

                {0,1,0.9,0.9},
                {1,0,0.9,0.9},
                {0,1,0,1},
                {0,1,1,0},

                {1,0,0,1},
                {1,0,1,0}

        };
	}

}
