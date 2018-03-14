import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

import java.util.ArrayList;

public class Boid
{
    public PVector velocity;
    public PVector position;
    public PVector normal = new PVector();
    float sightInner;
    float sightOuter;
    private float mass;
    private float maxVel;
    private float maxForce;
    private PVector acceleration;
    private PApplet app;

    public Boid(PApplet _app)
    {
        mass = 1f;
        maxVel = -1f;
        acceleration = new PVector();
        velocity = new PVector();
        position = new PVector();
        app = _app;
    }

    public Boid(float _mass, float _maxVel, float _maxForce, PVector _acceleration, PVector _velocity, PVector _position, PApplet _app)
    {
        mass = _mass;
        maxVel = _maxVel;
        maxForce = _maxForce;
        acceleration = _acceleration;
        velocity = _velocity;
        position = _position;
        app = _app;
    }

    public Boid(float _mass, PVector _position, float _maxVel, float _maxForce, PVector n, float _sightOuter, PApplet _app)
    {
        maxVel = _maxVel;
        maxForce = _maxForce;
        mass = _mass;
        position = _position;
        acceleration = new PVector();
        velocity = new PVector();
        app = _app;
        normal = n;
        sightOuter = _sightOuter;
        sightInner = app.random(0.4f * sightOuter, 0.9f * sightOuter);
    }

    public void addForce(PVector force)
    {
        if (force.mag() > maxForce && maxForce != -1) force.setMag(maxForce);
        acceleration.add(PVector.div(force, mass));
    }

    public void attract(PVector target)
    {
        PVector desired = PVector.sub(target, position);
        PVector steer = PVector.sub(desired, velocity);
        addForce(steer);
    }

    public void twist(Plane twistingPlane)
    {
        //cross the vector that points to the plane w the planes normal
        PVector agentToPlane = PVector.sub(twistingPlane.origin, position);
        PVector twist = agentToPlane.cross(twistingPlane.z);
        twist.setMag(maxVel);
        float dist = PVector.dist(twistingPlane.origin, position);
        twist.mult(1000 / (dist));
        PVector steer = PVector.sub(twist, velocity);

        addForce(steer);
    }

    public void followMeshNoiseField(Mesh m, KDTree meshVertexTree, float weight, boolean follow)
    {

        final PVector[] cp = m.closestPointOnMesh(position, meshVertexTree);
        this.normal = cp[1].copy();
        //Directly add some acceleration that is in the direction of the target, proportional to the distance
        PVector tempAccel = PVector.sub(cp[0], position);
        tempAccel.normalize();
        tempAccel.mult(weight);
        acceleration.add(tempAccel);

        if (follow)
        {
            PVector tex = cp[2].copy();

            tex.setMag(maxVel);
            PVector steer = PVector.sub(tex, velocity);

            addForce(steer);
        }


    }

    public void cohesion(KDTree pointsTree, float radius)
    {
        ArrayList<PVector> neighbours = pointsTree.radiusNeighbours(position, radius);
        if (neighbours.size() != 0)
        {
//            for (PVector neighbour : neighbours)
//            {
//                app.line(position.x, position.y, position.z, neighbour.x, neighbour.y, neighbour.z);
//            }


            //get sum of neighbours pos
            PVector steer;
            PVector desired = new PVector();
            for (PVector element : neighbours)
            {
                desired.add(PVector.mult(PVector.sub(element, position), element.dist(position)));
            }
            desired.div(neighbours.size());

            steer = PVector.sub(desired, velocity);

            addForce(steer);

        }

    }

    public void align(KDTree pointsTree, ArrayList<Boid> boidsList, ArrayList<PVector> positionList)
    {
        float radius = 260;
        ArrayList<PVector> neighbours = pointsTree.radiusNeighbours(position, radius);
        if (neighbours.size() != 0)
        {
//            if (SynthMain.drawnNeighbours)
//            {
//                for (PVector neighbour : neighbours)
//                {
//                    app.line(position.x, position.y, position.z, neighbour.x, neighbour.y, neighbour.z);
//                }
//            }

            //get sum of neighbours pos
            PVector sum = new PVector();
            for (PVector element : neighbours)
            {
                int index = positionList.indexOf(element);

                sum.add(boidsList.get(index).velocity);
            }
            sum.div(neighbours.size());
            PVector desired;
            desired = sum;


            PVector steer = PVector.sub(desired, velocity);

            addForce(steer);
        }

    }

    public void wanderOnMesh(float r, Mesh m)
    {
        PVector randVector = PVector.fromAngle((app.random(PConstants.TWO_PI)));
        randVector.setMag(r);
        Plane currentPlane = new Plane(position, normal);
        PVector transformedRandVector = (PVector.mult(currentPlane.x, randVector.x));
        transformedRandVector.add(PVector.mult(currentPlane.y, randVector.y));
        transformedRandVector.add(PVector.mult(currentPlane.z, randVector.z));
        PVector desired = (PVector.add(randVector, velocity));
        PVector steer = PVector.sub(desired, velocity);
        addForce(steer);


    }

    public void cohesionRepulsion(KDTree pointsTree)
    {
        int attractioncount = 0;
        ArrayList<PVector> neighbours = pointsTree.radiusNeighbours(position, this.sightOuter);
        PVector desiredRepulsion = new PVector();
        PVector desiredAttraction = new PVector();
        for (PVector element : neighbours)
        {
            if (element.dist(position) < this.sightInner)
            {
                if (SynthMain.drawneighbours)
                {
                    app.stroke(255, 0, 0);
                    app.line(position.x, position.y, position.z, element.x, element.y, element.z);
                }
                //Contribute to repulsion total
                PVector desired = PVector.sub(position, element);
                desired.mult(1 / position.dist(element));
                desiredRepulsion.add(desired);
            }
            else
            {
                if (SynthMain.drawneighbours)
                {

                    app.stroke(0, 0, 255);
                    app.line(position.x, position.y, position.z, element.x, element.y, element.z);
                }

                //Contribute to attraction total;
                desiredAttraction.add(element);
                attractioncount++;
            }
        }
        if (attractioncount != 0)
        {
            desiredAttraction.div(attractioncount);

        }
        desiredAttraction = PVector.sub(desiredAttraction, position);
        PVector steerAway = PVector.sub(desiredRepulsion, velocity);
        PVector steerToward = PVector.sub(desiredAttraction, velocity);
        addForce(steerAway);

        addForce(steerToward);


    }

    public void repulsion(KDTree pointsTree, float radius)
    {
        ArrayList<PVector> neighbours = pointsTree.radiusNeighbours(position, radius);

        if (neighbours.size() != 0)
        {
//            for (PVector neighbour : neighbours)
//            {
//                app.line(position.x, position.y, position.z, neighbour.x, neighbour.y, neighbour.z);
//            }
//

            //get sum of neighbours pos
            PVector steer;
            PVector desired = new PVector();
            for (PVector element : neighbours)
            {
                desired.add(PVector.mult(PVector.sub(element, position), radius / element.dist(position)));

            }
            desired.div(neighbours.size());
            desired.mult(-1);

            steer = PVector.sub(desired, velocity);

            addForce(steer);

            // if something is closer, its contribution to the sum ought to be greater

        }

    }


    public void integrate()
    {
        velocity.add(acceleration);
        if (maxVel > 0)
        {
            velocity.limit(maxVel);
        }
        position.add(velocity);
        acceleration = new PVector();
    }

    public void draw(float scalar)
    {
        app.pushMatrix();
        app.translate(position.x, position.y, position.z);

        app.stroke(255, 0, 0);
        app.noFill();
        //Plane pl = new Plane(new PVector(0, 0, 0), normal, velocity);
        PVector v = velocity.copy();
        v.normalize();
        app.line(0, 0, 0, scalar * v.x, scalar * v.y, scalar * v.z);

        app.popMatrix();
    }

}
