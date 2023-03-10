package nuti.agents;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public abstract class Human {

    protected ContinuousSpace<Object> space;
    protected Grid<Object> grid;
    protected int timeWorked;
    protected int timeEntertained;
    protected boolean usesMask;
    private GridPoint gridHousePoint;
    protected int step;

    public Human(ContinuousSpace<Object> space, Grid<Object> grid, int timeWorked, int timeEntertained, boolean usesMask) {
        this.space = space;
        this.grid = grid;
        this.timeWorked = timeWorked;
        this.timeEntertained = timeEntertained;
        this.usesMask = usesMask;
    }

    protected abstract boolean isInfected();

    protected abstract boolean isCured();

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        step++;
        GridPoint pt = grid.getLocation(this);

        GridCellNgh<Workplace> nghCreator = new GridCellNgh<Workplace>(grid, pt, Workplace.class, 1, 1);
        List<GridCell<Workplace>> gridCells = nghCreator.getNeighborhood(true);

        GridCellNgh<Entertainment> nghCreator2 = new GridCellNgh<Entertainment>(grid, pt, Entertainment.class, 1, 1);
        List<GridCell<Entertainment>> gridCells2 = nghCreator2.getNeighborhood(true);
        
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
        SimUtilities.shuffle(gridCells2, RandomHelper.getUniform());

        GridPoint pointWithWorkPlace = null;
        GridPoint pointWithEntertainment = null;


        // random decision to go to work or to entertainment place or to stay at home
        int random = RandomHelper.nextIntFromTo(0, 2);
        if (random == 0) {
            int maxCount = -1;
            for (GridCell<Workplace> cell : gridCells) {
                if (cell.size() > maxCount) {
                    pointWithWorkPlace = cell.getPoint();
                    maxCount = cell.size();
                }
            }
            moveTowards(pointWithWorkPlace);
            setTimeWorked(getTimeWorked() + 1);
//            System.out.println("getTimeWorked() = " + getTimeWorked());
        } else if (random == 1) {
            int maxCount = -1;
            for (GridCell<Entertainment> cell : gridCells2) {
                if (cell.size() > maxCount) {
                    pointWithEntertainment = cell.getPoint();
                    maxCount = cell.size();
                }
            }
            moveTowards(pointWithEntertainment);
            setTimeEntertained(getTimeEntertained() + 1);
//            System.out.println("getTimeEntertained() = " + getTimeEntertained());
        } else {
//            System.out.println("Stay at home");
            space.moveTo(this, gridHousePoint.getX(), gridHousePoint.getY());
        }

        if (this.isInfected()) {
            infect();
        }

        if (this.isCured()) {
            cure();
        }

    }
    
    @ScheduledMethod(start=8063, interval=0)
    public void printEnding() {
        Context<Object> context = ContextUtils.getContext(this);
        int infected = context.getObjects(SickHuman.class).size();
        int healthy = context.getObjects(HealthyHuman.class).size();
        int total = infected + healthy;
    	System.out.println("TOTAL INFECTADOS: " + (float)infected * 100/total + "%");
    	System.out.println("TOTAL SANOS: " + (float)healthy*100/total + "%");	
    }


    public void infect() {
        GridPoint pt = grid.getLocation(this);
        List<Object> healthyHuman = new ArrayList<Object>();
        for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
            if (obj instanceof HealthyHuman) {
                healthyHuman.add(obj);
            }
        }

        if (healthyHuman.size() > 0) {
            int index = RandomHelper.nextIntFromTo(0, healthyHuman.size() - 1);
            Object obj = healthyHuman.get(index);
            NdPoint spacePt = space.getLocation(obj);

            Context<Object> context = ContextUtils.getContext(obj);
            context.remove(obj);
            SickHuman sickHuman = new SickHuman(space, grid, this.timeWorked, this.timeEntertained, false);
            sickHuman.setGridHousePoint(this.getGridHousePoint());
            context.add(sickHuman);
            space.moveTo(sickHuman, spacePt.getX(), spacePt.getY());
            grid.moveTo(sickHuman, pt.getX(), pt.getY());

        }
    }

    public void cure() {
        GridPoint pt = grid.getLocation(this);
        NdPoint spacePt = space.getLocation(this);
        Context<Object> context = ContextUtils.getContext(this);
        context.remove(this);
        HealthyHuman healthyHuman = new HealthyHuman(space, grid, this.timeWorked, this.timeEntertained, false);
        healthyHuman.setGridHousePoint(this.getGridHousePoint());
        context.add(healthyHuman);
        space.moveTo(healthyHuman, spacePt.getX(), spacePt.getY());
        grid.moveTo(healthyHuman, pt.getX(), pt.getY());
    }


    private void moveTowards(GridPoint pt) {
        NdPoint myPoint = space.getLocation(this);
        NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
        double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
        space.moveByVector(this, 1, angle, 0);
        grid.moveTo(this, (int) space.getLocation(this).getX(), (int) space.getLocation(this).getY());
    }


    public NdPoint getLocation() {
        return space.getLocation(this);
    }

    public void move(int x, int y) {
        grid.moveTo(this, x, y);
    }

    public ContinuousSpace<Object> getSpace() {
        return space;
    }

    public void setSpace(ContinuousSpace<Object> space) {
        this.space = space;
    }

    public Grid<Object> getGrid() {
        return grid;
    }

    public void setGrid(Grid<Object> grid) {
        this.grid = grid;
    }

    public int getTimeWorked() {
        return timeWorked;
    }

    public void setTimeWorked(int timeWorked) {
        this.timeWorked = timeWorked;
    }

    public int getTimeEntertained() {
        return timeEntertained;
    }

    public void setTimeEntertained(int timeEntertained) {
        this.timeEntertained = timeEntertained;
    }

    public boolean isUsesMask() {
        return usesMask;
    }

    public void setUsesMask(boolean usesMask) {
        this.usesMask = usesMask;
    }

    public GridPoint getGridHousePoint() {
        return gridHousePoint;
    }

    public void setGridHousePoint(GridPoint gridHousePoint) {
        this.gridHousePoint = gridHousePoint;
    }


}
