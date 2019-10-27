/*
 Notes for model 2
 Customer = (A, S, AT, M, Q)
 A = arrival time, S = service time with teller, AT = scheduled appointment time with officer, M = length of meeting with officer, Q = quit variable
 Curr = current time (time of the current event)
 q = customer queue (FIFO)
 PQ = priority queue for customers waiting to see officer
 ST = stack of tasks
 E = event list
 Cust = next customer to arrive (already read in from customer arrivals file)
 NextTask = next task to arrive (already read from task arrivals file)
 currTask = task teller 3 is now working on
 c_i = customer now at teller i (1 <= i <= 3)
 nextQtime = time of next scheduled type 4 (quit) event
 nextQcust = customer next scheduled to quit
 
 Event 1, 2, 3:  c_i completes service at teller i (i = 1,2,3)
 Customer c_i finishes service at teller i. Compute throughtime and add to total. If M > 0, reset S = 0, then enqueue on PQ. If q is nonempty, dequeue the next customer to c_i to begin service at teller i. Prepare the next type i exit event for this customer at time Curr + S. If this customer happens to equal nextQcust, then the quit event for this customer will not be carried out. The type 4 quit event for this customer should be deleted from the event list using E.del(Event(nextQtime, 4)). It is also necessary to traverse the queue to find the next customer to quit, re-compute nextQtime, nextQcust, and insert a quit event for nextQcust into the event list E.
 
 For teller 3, if q is empty and ST is not empty, pop from ST to currTask and begin work on that task. Prepare a type 5 task completion event for this task.
 Event 5: task completion
 If ST is not empty, pop from ST to currTask and prepare the next type 5 event (as above).
 If ST is empty and q is not empty, dequeue the next customer to c_3 to begin teller service and prepare a type 3 exit event for this customer for time Curr + S.
 Event 6: task arrival
 ST.push(NextTask). If the task file is nonempty, read the next task into NextTask and prepare the next type 6 event for this task.
 Event 7: customer cust arrival
 If M > 0 and S = 0, enqueue on PQ.
 If M > 0, S > 0, and  Q >= 0, enqueue on PQ
 If M > 0, S > 0, and Q = -1, reset Q = AT – 1 and enqueue on q. If Q < nextQtime, reset nextQtime = Q, nextQcust =  c, delete existing quit event, schedule quit event for c.
 If M = 0, then S > 0; reset Q = AT + Q and enqueue on q. If Q < nextQtime, follow steps given above to adjust for next quit event.
 If a customer enqueues on a previously empty q and a teller is free, the customer immediately dequeues to the first free teller (in order 1, 2, 3) to begin service. Prepare an exit event for this customer.
 If the customer arrivals file is nonempty, read the next record into cust and prepare the next type 7 event for this customer.
 Event 8: meeting completion
 If S = 0, the customer leaves the bank; compute the throughtime.
 If S > 0, reset M = 0, enqueue on q and follow the steps for updating the next quit event.
 Event 4: quit event
 Remove customer nextQcust from q. Traverse the queue to identify the customer, C, with the earliest quitting time (minimum value of positive Q). For this customer, set nextQcust = C, nextQtime = Q, and schedule the next type 4 event for this customer.
 */
import java.util.*;
import java.io.*;
import java.lang.*;
import java.text.*;
public class BankSimulation{
    public static void main(String[] args)throws IndexOutOfBoundsException, FileNotFoundException,Exception, IOException, ParseException, NoSuchElementException,NullPointerException{
        int currTime = 850;
        int N = 0;
        int tt = 0;
        int TT = 0;
        System.out.println("\n\n *** BEGINNING SIMULATION *** \n\n");
        
        // Task file, Customer file
        File in1 = new File("customerArrivalFiles.txt"); //read from this file
        File in2 = new File("taskFile.txt"); //read from this file
        Scanner sc1 = new Scanner(in1);
        Scanner sc2 = new Scanner(in2);
        
        // Customer Queues
        Queue2<Customer> Q = new Queue2<Customer>();
        Queue2<Customer> Q1 = new Queue2<Customer>();
        Q.enqueue(new Customer(835,2,0,0,1000)); Q.enqueue(new Customer(830,5,832,0,1000));
        Q.enqueue(new Customer(840,3,0,0,1000));
        
        // EventList
        OList<Event> E = new OList<Event>();
        E.insert(new Event(850,7)); E.insert(new Event(900,1)); E.insert(new Event(905,6));
        E.insert(new Event(907,2)); E.insert(new Event(910,5)); E.insert(new Event(910,8));
        
        // Stack of Tasks
        Stack1gen<Task> ST = new Stack1gen<>();
        ST.push(new Task(840,3)); ST.push(new Task(845,7));
        
        // Priority Queueu
        Heap1customer PQ = new Heap1customer();
        PQ.enqueue(new Customer(830,0,840,10,0));PQ.enqueue(new Customer(825,0,845,5,0));
        PQ.enqueue(new Customer(835,5,830,10,1000));PQ.enqueue(new Customer(824,0,820,10,0));
        PQ.enqueue(new Customer(823,0,846,5,0));PQ.enqueue(new Customer(840,0,839,8,0));
        
        // Tellers
        Customer T1 = new Customer(830,5,0,0,0);
        Customer T2 = new Customer(840,5,0,0,0);
        Customer T3 = null;
        // Teller3 task handle
        Task T3CurrentTask = new Task(840,10);
        
        // Manager
        Customer Manager = new Customer(825,0,830,15,0);
        
        // next task
        Task nextTask = new Task(905,5);
        // next customer
        Customer nextCustomer = new Customer(850,7,0,0,1000);
        // next quit time
        int nextQTime = 9999;
        // next quit customer
        Customer nextQCust = new Customer(0,0,0,0,9999);
        // cust i
        Customer iC;
        // Temp handling customer that quit
        Customer currQuitingC;

        Event currEvent;
        int numEvents = 0;
        
        while(E.getSize() > 0){
            
            currEvent =  E.getData(1);
            currTime = currEvent.getTime();
            
            // Event 1, 2, 3:  c_i completes service at teller i (i = 1,2,3)
            
            // Customer c_i finishes service at teller i.
            // Compute throughtime and add to total. If M > 0, reset S = 0, then enqueue on PQ.
            // If q is nonempty, dequeue the next customer to c_i to begin service at teller i. Prepare the next type i exit event for this customer at time Curr + S.
            // If this customer happens to equal nextQcust, then the quit event for this customer will not be carried out. The type 4 quit event for this customer should be deleted from the event list using E.del(Event(nextQtime, 4)). It is also necessary to traverse the queue to find the next customer to quit, re-compute nextQtime, nextQcust, and insert a quit event for nextQcust into the event list E.
            
            // Customer exits T1
            if(currEvent.getType() == 1){
                currTime = currEvent.getTime();
                System.out.println("Current time: " + currTime + "  :  " + T1.toString() + " exits T1");
                // Compute throughtime and add to total.
                iC = T1;
                
                // If M > 0, reset S = 0, then enqueue on PQ.
                if(iC.getAppointmentLength() > 0){
                    iC.setServiceTime(0);
                    PQ.enqueue(iC);
                    iC = null;
                    T1 = null;
                }
                else{
                    iC = null;
                    N++;
                    tt = currTime - T1.getArrivalTime() - (currTime/100 - T1.getArrivalTime()/100)*40;
                    // System.out.println("tt = " + tt + "currtime " + currTime + " arrivalTime" + T1.getArrivalTime());
                    TT+=tt;
                    // N++;
                    T1 = null;
                }
                // If q is nonempty, dequeue the next customer to c_i to begin service at teller i. Prepare the next type i exit event for this customer at time Curr + S.
                if(Q.getSize() > 0){
                    T1 = Q.dequeue();
                    E.insert(new Event(computeTime(currTime,T1.getServiceTime()), 1));
                    
                    // If this customer happens to equal nextQcust, then the quit event for this customer will not be carried out.
                    // The type 4 quit event for this customer should be deleted from the event list using E.del(Event(nextQtime, 4)).
                    if(T1.equals(nextQCust) == true && T1.getQuitTime() == nextQTime){
                        // The type 4 quit event for this customer should be deleted from the event list using
                        E.del(new Event(T1.getQuitTime(), 4));
                        
                        // It is also necessary to traverse the queue to find the next customer to quit, re-compute nextQtime, nextQcust, and insert a quit event for nextQcust into the event list E.
                        nextQCust = new Customer(0,0,0,0,9999);
                        nextQTime = 9999;
                        while(Q.getSize() > 0){
                            if(Q.getFrontData().getQuitTime() < nextQTime && Q.getFrontData().getQuitTime() != 1000){
                                nextQTime = Q.getFrontData().getQuitTime();
                                nextQCust = Q.getFrontData();
                                Q1.enqueue(Q.dequeue());
                            }
                            else{
                                Q1.enqueue(Q.dequeue());
                            }
                        }
                        while(Q1.getSize() > 0){
                            Q.enqueue(Q1.dequeue());
                        }
                        
                        if(nextQTime != 9999){
                            E.insert(new Event(nextQTime, 4));
                        }
                    }
                }
            }
            
            // Customer exits T2
            else if(currEvent.getType() == 2){
                currTime = currEvent.getTime();
                System.out.println("Current time: " + currTime + "  :  " + T2.toString() + " exits T2");
                // Compute throughtime and add to total.
                iC = T2;
                // If M > 0, reset S = 0, then enqueue on PQ.
                if(iC.getAppointmentLength() > 0){
                    iC.setServiceTime(0);
                    PQ.enqueue(iC);
                    iC = null;
                    T2 = null;
                }
                else{
                    iC = null;
                    N++;
                    tt = currTime - T2.getArrivalTime() - (currTime/100 - T2.getArrivalTime()/100)*40;
                    // System.out.println("tt = " + tt + "currtime " + currTime + " arrivalTime" + T2.getArrivalTime());
                    TT+=tt;
                    // N++;
                    T2 = null;
                }

                // If q is nonempty, dequeue the next customer to c_i to begin service at teller i. Prepare the next type i exit event for this customer at time Curr + S.
                if(Q.getSize() > 0){
                    T2 = Q.dequeue();
                    E.insert(new Event(computeTime(currTime,T2.getServiceTime()), 2));
                    
                    // If this customer happens to equal nextQcust, then the quit event for this customer will not be carried out.
                    // The type 4 quit event for this customer should be deleted from the event list using E.del(Event(nextQtime, 4)).
                    if(T2.equals(nextQCust) == true && T2.getQuitTime() == nextQTime){
                        // The type 4 quit event for this customer should be deleted from the event list using
                        E.del(new Event(T2.getQuitTime(), 4));
                        
                        // It is also necessary to traverse the queue to find the next customer to quit, re-compute nextQtime, nextQcust, and insert a quit event for nextQcust into the event list E.
                        nextQCust = new Customer(0,0,0,0,9999);
                        nextQTime = 9999;
                        while(Q.getSize() > 0){
                            if(Q.getFrontData().getQuitTime() < nextQTime && Q.getFrontData().getQuitTime() != 1000){
                                nextQTime = Q.getFrontData().getQuitTime();
                                nextQCust = Q.getFrontData();
                                Q1.enqueue(Q.dequeue());
                            }
                            else{
                                Q1.enqueue(Q.dequeue());
                            }
                        }
                        while(Q1.getSize() > 0){
                            Q.enqueue(Q1.dequeue());
                        }
                        if(nextQTime != 9999){
                            E.insert(new Event(nextQTime, 4));
                        }
                    }
                }
            }
            
            // For teller 3, if q is empty and ST is not empty, pop from ST to currTask and begin work on that task. Prepare a type 5 task completion event for this task.
            // Customer exits T3
            else if(currEvent.getType() == 3){
                currTime = currEvent.getTime();
                System.out.println("Current time: " + currTime + "  :  " + T3.toString() + " exits T3");
                iC = T3;
                T3CurrentTask = null;

                // If M > 0, reset S = 0, then enqueue on PQ.
                if(iC.getAppointmentLength() > 0){
                    iC.setServiceTime(0);
                    PQ.enqueue(iC);
                    iC = null;
                }
                else{
                    iC = null;
                    N++;
                    tt = currTime - T3.getArrivalTime() - (currTime/100 - T3.getArrivalTime()/100)*40;
                    // System.out.println("tt = " + tt + "currtime " + currTime + " arrivalTime" + T3.getArrivalTime());
                    // N++;
                    TT+=tt;
                    T3 = null;
                }
                if(ST.getSize() > 0){
                    T3CurrentTask = ST.pop();
                    E.insert(new Event(computeTime(currTime,T3CurrentTask.getTaskServiceTime()), 5));
                    iC = null;
                }
                else if(Q.getSize() > 0){
                    iC = Q.getFrontData();
                    T3 = Q.dequeue();
                    
                    E.insert(new Event(computeTime(currTime,T3.getServiceTime()), 3));
                    
                    // If this customer happens to equal nextQcust, then the quit event for this customer will not be carried out.
                    // The type 4 quit event for this customer should be deleted from the event list using E.del(Event(nextQtime, 4)).
                    if(T3.equals(nextQCust) == true && T3.getQuitTime() == nextQTime){
                        // The type 4 quit event for this customer should be deleted from the event list using
                        E.del(new Event(T3.getQuitTime(), 4));
                        
                        // It is also necessary to traverse the queue to find the next customer to quit, re-compute nextQtime, nextQcust, and insert a quit event for nextQcust into the event list E.
                        nextQCust = new Customer(0,0,0,0,9999);
                        nextQTime = 9999;
                        while(Q.getSize() > 0){
                            if(Q.getFrontData().getQuitTime() < nextQTime && Q.getFrontData().getQuitTime() != 1000){
                                nextQTime = Q.getFrontData().getQuitTime();
                                nextQCust = Q.getFrontData();
                                Q1.enqueue(Q.dequeue());
                            }
                            else{
                                Q1.enqueue(Q.dequeue());
                            }
                        }
                        while(Q1.getSize() > 0){
                            Q.enqueue(Q1.dequeue());
                        }
                        if(nextQTime != 9999){
                            E.insert(new Event(nextQTime, 4));
                        }
                    }
                }
                else{
                    T3 = null;
                    T3CurrentTask = null;
                }
            }
            
            /*
             Event 4: quit event
             Remove customer nextQcust from q.
             Traverse the queue to identify the customer, C, with the earliest quitting time (minimum value of positive Q).
             For this customer, set nextQcust = C, nextQtime = Q, and schedule the next type 4 event for this customer
             */
            // 4 Customer quits queue
            
            else if(currEvent.getType() == 4){
                currTime = currEvent.getTime();
                System.out.println("Customer Quit (" + nextQCust.toString());
                // Remove customer nextQcust from q.
                currQuitingC = Q.remove(nextQCust);
                
                if(currQuitingC.getAppointmentLength() > 0){
                    PQ.enqueue(currQuitingC);
                }
                
                // Traverse the queue to identify the customer, C, with the earliest quitting time (minimum value of positive Q).
                // For this customer, set nextQcust = C, nextQtime = Q, and schedule the next type 4 event for this customer
                // nextQCust = null;
                nextQCust = new Customer(0,0,0,0,9999);
                nextQTime = 9999;
                while(Q.getSize() > 0){
                    if(Q.getFrontData().getQuitTime() < nextQTime &&  Q.getFrontData().getQuitTime() != 1000){
                        nextQTime = Q.getFrontData().getQuitTime();
                        nextQCust = Q.getFrontData();
                        Q1.enqueue(Q.dequeue());
                    }
                    else{
                        Q1.enqueue(Q.dequeue());
                    }
                }
                while(Q1.getSize() > 0){
                    Q.enqueue(Q1.dequeue());
                }
                if(nextQTime != 9999){
                    E.insert(new Event(nextQTime, 4));
                }
            }
            
            // Event 5: task completion
            // If ST is not empty, pop from ST to currTask and prepare the next type 5 event (as above).
            // If ST is empty and q is not empty, dequeue the next customer to c_3 to begin teller service and prepare a type 3 exit event for this customer for time Curr + S.
            // Task completion
            else if(currEvent.getType() == 5){
                currTime = currEvent.getTime();
                System.out.print("Current time: " + currTime + "  :  ");
                System.out.println("Task completion (" + T3CurrentTask.getTaskArrivalTime() + " " + T3CurrentTask.getTaskServiceTime() + ")");
 
                T3 = null;
                T3CurrentTask = null;
                
                if(ST.getSize() > 0){
                    T3CurrentTask = ST.pop();
                    E.insert(new Event(currTime + T3CurrentTask.getTaskServiceTime(), 5));
                }
                else if(Q.getSize() > 0){
                    
                    iC = Q.getFrontData();
                    T3 = Q.dequeue();
                    
                    E.insert(new Event(computeTime(currTime,T3.getServiceTime()), 3));
                    
                    // If this customer happens to equal nextQcust, then the quit event for this customer will not be carried out.
                    // The type 4 quit event for this customer should be deleted from the event list using E.del(Event(nextQtime, 4)).
                    if(T3.equals(nextQCust) == true && T3.getQuitTime() == nextQTime){
                        // The type 4 quit event for this customer should be deleted from the event list using
                        E.del(new Event(T3.getQuitTime(), 4));
                        
                        // It is also necessary to traverse the queue to find the next customer to quit, re-compute nextQtime, nextQcust, and insert a quit event for nextQcust into the event list E.
                        nextQCust = new Customer(0,0,0,0,9999);
                        nextQTime = 9999;
                        while(Q.getSize() > 0){
                            if(Q.getFrontData().getQuitTime() < nextQTime && Q.getFrontData().getQuitTime() != 1000){
                                nextQTime = Q.getFrontData().getQuitTime();
                                nextQCust = Q.getFrontData();
                                Q1.enqueue(Q.dequeue());
                            }
                            else{
                                Q1.enqueue(Q.dequeue());
                            }
                        }
                        while(Q1.getSize() > 0){
                            Q.enqueue(Q1.dequeue());
                        }
                        if(nextQTime != 9999){
                            E.insert(new Event(nextQTime, 4));
                        }
                    }
                }
                else{
                    T3 = null;
                    T3CurrentTask = null;
                }
            }
            
            // Task arrival
            // Event 6: task arrival
            // ST.push(NextTask). If the task file is nonempty, read the next task into NextTask and prepare the next type 6 event for this task.
            else if(currEvent.getType() == 6){
                currTime = currEvent.getTime();
                System.out.print("Current time: " + currTime + "  :  ");
                System.out.println("Task Arrival (" + nextTask.getTaskArrivalTime() + " " + nextTask.getTaskServiceTime() + ")");
                
                if(ST.getSize() == 0 && T3CurrentTask == null && T3 == null){
                    T3CurrentTask = nextTask;
                    E.insert(new Event(currTime + T3CurrentTask.getTaskServiceTime(), 5));
                    if(sc2.hasNext()){
                        nextTask = new Task(sc2.nextInt(),sc2.nextInt());
                        E.insert(new Event(nextTask.getTaskArrivalTime(), 6));
                    }
                }
                else{
                    ST.push(nextTask);
                    nextTask = null;
                    if(sc2.hasNext()){
                        nextTask = new Task(sc2.nextInt(),sc2.nextInt());
                        E.insert(new Event(nextTask.getTaskArrivalTime(), 6));
                    }
                }
            }
            
            /*Event 7: customer cust arrival
             If M > 0 and S = 0, enqueue on PQ.
             If M > 0, S > 0, and  Q >= 0, enqueue on PQ
             If M > 0, S > 0, and Q = -1, reset Q = AT – 1 and enqueue on q. If Q < nextQtime, reset nextQtime = Q, nextQcust =  c, delete existing quit event, schedule quit event for c.
             If M = 0, then S > 0; reset Q = AT + Q and enqueue on q. If Q < nextQtime, follow steps given above to adjust for next quit event.
             If a customer enqueues on a previously empty q and a teller is free, the customer immediately dequeues to the first free teller (in order 1, 2, 3) to begin service. Prepare an exit event for this customer.
             If the customer arrivals file is nonempty, read the next record into cust and prepare the next type 7 event for this customer.
             */
            
            // Type 7: Customer arrival
            else if(currEvent.getType() == 7){
                currTime = currEvent.getTime();
                //System.out.println("Current time: " + currTime + "  :  Customer Arrival (" + nextCustomer.toString());
                System.out.println("Current time: " + currTime + "  :  Customer Arrival (" + nextCustomer.toString());
                // If M > 0 and S = 0, enqueue on PQ.
                // or If M > 0, S > 0, and  Q >= 0, enqueue on PQ
                if((nextCustomer.getAppointmentLength() > 0 && nextCustomer.getServiceTime() == 0)||
                   (nextCustomer.getAppointmentLength() > 0 && nextCustomer.getServiceTime() > 0 && nextCustomer.getQuitTime() >= 0)){
                    PQ.enqueue(nextCustomer);
                }
                
                // If M > 0, S > 0, and Q = -1, reset Q = AT – 1 and enqueue on q.
                // If Q < nextQtime, reset nextQtime = Q, nextQcust =  c, delete existing quit event, schedule quit event for c.
                else if(nextCustomer.getAppointmentLength() > 0 && nextCustomer.getServiceTime() > 0 && nextCustomer.getQuitTime() == -1){
                    // reset Q = AT – 1 and enqueue on q.
                    nextCustomer.setQuitTime(nextCustomer.getAppointmentTime() - 1);
                    Q.enqueue(nextCustomer);
                    
                    if(Q.getSize() == 1){
                        if(T1 == null){
                            T1 = Q.dequeue();
                            E.insert(new Event(computeTime(T1.getArrivalTime(),T1.getServiceTime()), 1));
                        }
                        else if(T2 == null){
                            T2 = Q.dequeue();
                            E.insert(new Event(computeTime(T2.getArrivalTime(),T2.getServiceTime()), 2));
                        }
                        else if(T3 == null && T3CurrentTask == null){
                            T3 = Q.dequeue();
                            E.insert(new Event(computeTime(T3.getArrivalTime(),T3.getServiceTime()), 3));
                        }
                        else{
                        }
                    }
                    // If Q < nextQtime, reset nextQtime = Q, nextQcust =  c, delete existing quit event, schedule quit event for c.
                    if(nextCustomer.getQuitTime() < nextQTime && nextCustomer.getQuitTime() != 1000 && nextCustomer.equals(T1) != true && nextCustomer.equals(T2) != true && nextCustomer.equals(T3) != true){
                        //delete existing quit event
                        if(nextQTime != 9999){
                        E.del(new Event(nextQTime, 4));
                        }
                        //nextQtime = Q, nextQcust =  c,
                        nextQTime = nextCustomer.getQuitTime();
                        nextQCust = nextCustomer;
                        // schedule quit event for c
                        E.insert(new Event(nextQTime, 4));
                    }
                }
                
                // If M = 0, then S > 0; reset Q = AT + Q and enqueue on q.
                // If Q < nextQtime, follow steps given above to adjust for next quit event.
                else if(nextCustomer.getAppointmentLength() == 0)// && nextCustomer.getServiceTime() > 0) // then service time is > 0
                {
                    if(nextCustomer.getQuitTime() != 1000)// && nextCustomer.getQuitTime() != -1)
                    {
                    nextCustomer.setQuitTime(computeTime(nextCustomer.getArrivalTime(),nextCustomer.getQuitTime()));
                    }
                    Q.enqueue(nextCustomer);

                    // If a customer enqueues on a previously empty q and a teller is free, the customer immediately dequeues to the first free teller (in order 1, 2, 3) to begin service.
                    // Prepare an exit event for this customer.
                    
                    if(Q.getSize() == 1){
                        if(T1 == null){
                            T1 = Q.dequeue();
                            E.insert(new Event(computeTime(T1.getArrivalTime(),T1.getServiceTime()), 1));
                        }
                        else if(T2 == null){
                            T2 = Q.dequeue();
                            E.insert(new Event(computeTime(T2.getArrivalTime(),T2.getServiceTime()), 2));
                        }
                        else if(T3 == null && T3CurrentTask == null){
                            T3 = Q.dequeue();
                            E.insert(new Event(computeTime(T3.getArrivalTime(),T3.getServiceTime()), 3));
                        }
                        else{
                        }
                    }
                    
                    if(nextCustomer.getQuitTime() < nextQTime && nextCustomer.getQuitTime() != 1000 && nextCustomer.equals(T1) != true && nextCustomer.equals(T2) != true && nextCustomer.equals(T3) != true){
                        if(nextQTime!=9999){
                        E.del(new Event(nextQTime, 4));
                        }
                        //nextQtime = Q, nextQcust =  c,
                        nextQTime = nextCustomer.getQuitTime();
                        nextQCust = nextCustomer;
                        //delete existing quit event, schedule quit event for c
                        E.insert(new Event(nextQTime, 4));
                    }
                }
                else{
                }
                // If the customer arrivals file is nonempty, read the next record into cust and prepare the next type 7 event for this customer.
                if(sc1.hasNext()){
                    nextCustomer = new Customer(sc1.nextInt(),sc1.nextInt(),sc1.nextInt(),sc1.nextInt(),sc1.nextInt());
                    E.insert(new Event(nextCustomer.getArrivalTime(), 7));
                }
            }
            
            /*
             Event 8: meeting completion
             If S = 0, the customer leaves the bank; compute the throughtime.
             If S > 0, reset M = 0, enqueue on q and follow the steps for updating the next quit event.
             */
            
            // type 8: Appointment Completion
            else{
                currTime = currEvent.getTime();
                //System.out.println("Current time: " + currTime + "  :  Appt Completion (" + Manager.toString());

                // If S = 0, the customer leaves the bank; compute the throughtime.
                if(Manager.getServiceTime() == 0){
                    tt = currTime - Manager.getArrivalTime() - (currTime/100 - Manager.getArrivalTime()/100)*40;
                    // System.out.println("tt = " + tt + " currtime " + currTime + " arrivalTime" + Manager.getArrivalTime());
                    TT+=tt;
                    N++;
                }
                // If S > 0, reset M = 0, enqueue on q and follow the steps for updating the next quit event.
                else if(Manager.getServiceTime() > 0){
                    Manager.setAppointmentLength(0);
                    Q.enqueue(Manager);

                    // If a customer enqueues on a previously empty q and a teller is free, the customer immediately dequeues to the first free teller (in order 1, 2, 3) to begin service.
                    // Prepare an exit event for this customer.
                    
                    if(Q.getSize() > 0){
                        if(T1 == null){
                            T1 = Q.dequeue();
                            //System.out.println("T1" + T1.toString());
                            E.insert(new Event(computeTime(currTime,T1.getServiceTime()), 1));
                        }
                        else if(T2 == null){
                            T2 = Q.dequeue();
                            //System.out.println("T2" + T2.toString());
                            E.insert(new Event(computeTime(currTime,T2.getServiceTime()), 2));
                        }
                        else if(T3 == null && T3CurrentTask == null){
                            T3 = Q.dequeue();
                            //System.out.println("T3" + T3.toString());
                            E.insert(new Event(computeTime(currTime,T3.getServiceTime()), 3));
                        }
                        else{
                        }
                    }
                }
                else{
                    
                }
                System.out.println("Current time: " + currTime + "  :  Appt Completion (" + Manager.toString());

                if(PQ.getSize()>0){
                    Manager = PQ.dequeue();
                    E.insert(new Event(computeTime(currTime,Manager.getAppointmentLength()), 8));
                }
            }

            E.del(E.getData(1));
            numEvents++;
        }
        sc1.close();//closing files
        sc2.close();//closing files
        
        
        System.out.println("\n\n *** SIMULATION RESULTS *** \n\n");
        System.out.println("Start time                  : " + 8 + ":" + 50 + " AM");
        System.out.println("End time                    : " + currTime / 100 + ":" + currTime % 100 + " AM");
        System.out.println("Number of customers         : " + N + " Customers");
        System.out.println("Average turn around time    : " + TT/N + " Minutes");
        System.out.println("Total turn around time      : " + TT + " Minutes");
        System.out.println("Total num of events         : " + numEvents + " Events");
        System.out.println("Average event time          : " + (currTime - 850) / numEvents + " Minutes\n\n");
    }
    // currTime , extraTime
    public static int computeTime(int t, int t1){
        int h = t/100;
        int m = t%100;
        
        h = h + (m + t1)/60;
        m = (m+t1)%60;
        
        return 100 * h + m;
    }
}

// Customer.java
class Customer implements Comparable<Customer>{
    private int arrivalTime;
    private int serviceTime;
    private int appointmentTime;
    private int appointmentLength;
    private int quitTime;
    
    public Customer(int arrivalTime,int serviceTime, int appointmentTime,int appointmentLength, int quitTime){
        this.arrivalTime = arrivalTime;
        this.serviceTime = serviceTime;
        this.appointmentTime = appointmentTime;
        this.appointmentLength = appointmentLength;
        this.quitTime = quitTime;
    }
    public int getArrivalTime(){
        return this.arrivalTime;
    }
    public int getServiceTime(){
        return this.serviceTime;
    }
    public int getAppointmentTime(){
        return this.appointmentTime;
    }
    public int getAppointmentLength(){
        return this.appointmentLength;
    }
    public int getQuitTime(){
        return this.quitTime;
    }
    public void setServiceTime(int sT){
        this.serviceTime = sT;
    }
    public void setAppointmentLength(int aL)
    {
        this.appointmentLength = aL;
    }
    public void setQuitTime(int qT){
        this.quitTime = qT;
    }
    public String toString(){
        return "Customer (" + this.getArrivalTime() + " , " + this.getServiceTime() + " , " + this.getAppointmentTime() + " , "  + this.getAppointmentLength() + " , " + this.getQuitTime() + ")";
    }
    public boolean equals(Customer c){
        if(c == null){
            return false;
        }
        else if(this.getArrivalTime() == c.getArrivalTime() && this.getServiceTime() == c.getServiceTime() && this.getAppointmentTime() == c.getAppointmentTime() && this.getAppointmentLength() == c.getAppointmentLength() && this.getQuitTime() == c.getQuitTime()){
            return true;
        }
        else{
            return false;
        }
    }
    public int compareTo(Customer c){
        if((this.getArrivalTime() <= this.getAppointmentTime() && c.getArrivalTime() <= c.getAppointmentTime() && this.getAppointmentTime() <= c.getAppointmentTime())
           || (this.getArrivalTime() <= this.getAppointmentTime() && c.getArrivalTime() > c.getAppointmentTime())
           || (this.getArrivalTime() > this.getAppointmentTime() && c.getArrivalTime() > c.getAppointmentTime() && this.getArrivalTime() <= c.getArrivalTime())){
            return -1;
        }
        else{
            return 1;
        }
    }
}

// Task.java
class Task{
    private int taskArrivalTime;
    private int taskServiceTime;
    public Task(int taskArrivalTime,int taskServiceTime){
        this.taskArrivalTime = taskArrivalTime;
        this.taskServiceTime = taskServiceTime;
    }
    public int getTaskArrivalTime(){
        return this.taskArrivalTime;
    }
    public int getTaskServiceTime(){
        return this.taskServiceTime;
    }
    public String toString(){
        return "Task: " + this.getTaskArrivalTime() + " , " + this.getTaskServiceTime();
    }
}
// Node2.java
class Node2<T>{
    private T data;
    private Node2 link;
    //constructor
    public Node2(T newData, Node2 linkValue){
        data = newData;
        link = linkValue;
    }
    public T getData(){
        return data;
    }
    public Node2 getLink(){
        return link;
    }
    public void setData(T y){
        data = y;
    }
    public void setLink(Node2 linkValue){
        link = linkValue;
    }
}
// Event.java
class Event implements Comparable<Event>{ //, PrintValue
    private int time, type;
    public Event(int timeE, int typeE){
        time = timeE;
        type = typeE;
    }
    public int getTime(){
        return time;
    }
    public int getType(){
        return type;
    }
    public void setTime(int timeE){
        time = timeE;
    }
    public void setType(int typeE){
        type = typeE;
    }
    public String toString(){
        return + time + "," + type;
    }
      public void printVal()
      {
        System.out.println("Event: " + "time: " + time + "  type: " + type);
     }
    public int compareTo(Event e){
        if((time < e.getTime()) ||
           ((time == e.getTime()) && (type < e.getType()))){
            return -1;
        }
        else if((time == e.getTime()) && (type == e.getType())){
            return 0;
        }
        else{
            return 1;
        }
    }
}
//OList.java
//Class to implement the Ordered List ADT for generic objects of type T
//The type T extends Comparable so that we can use the compareTo() method
class OList<T extends Comparable>{
    private Node2<T> start;//starting node of the list
    private int size;//size of the list
    //constructor
    public OList(){
        start = null;
        size = 0;
    }
    //copy constructor
    public OList(OList L){
        Node2<T> curr = L.getStart();
        T item = curr.getData();
        size = L.getSize();
        start = new Node2(item, null); //copy the first node
        Node2<T> save = start;
        Node2<T> newnode = null;
        for(int i =1; i<=size-1; i++){
            curr= curr.getLink();//update the current node to be copied from list L
            item = curr.getData();//retrieve the data of the current node
            newnode = new Node2(item,null);//copy the data of the current node
            //into a new node
            save.setLink(newnode);//the previous node on the copy list
            //points to the new node on the copy list
            save = newnode;//save the new node address for the next cycle
        }
    }
    public void insert(T y){
        Node2<T> curr=null;;
        Node2<T> save=null;
        Node2<T> newnode=null;
        //search for the correct order position of data value y
        curr = start;//initialize the search at the beginning of the list
        while((curr != null) && (curr.getData().compareTo(y)<0)){
            //if y comes after the current data, update the search
            save=curr;
            curr=curr.getLink();
        }
        newnode = new Node2(y,curr);//new node to hold inserted value
        if(curr==start){
            start = newnode; //insertion at the beginning of the list
        }
        else{
            save.setLink(newnode); //previous node points to the new value
        }
        size++;
    }
    
    public void del(T y){ //search for first occurance of value y, and remove
        Node2<T> curr=null;
        Node2<T> save=null;
        //search for first occurance of y
        curr = start;
        while((curr != null) && (curr.getData().compareTo(y) != 0)){
            save = curr;
            curr = curr.getLink();
        }
        if(curr == null){
            System.out.println("no deletion: " + y + " not on list");
        }
        else{
            size--;
            if(curr == start){
                start = curr.getLink();//deletion at the beginning
            }
            else{
                save.setLink(curr.getLink());//delete the current node:
                //previous node points to following node
            }
        }
    }
    public void printListBasic(){
        Node2<T> curr = start;
        System.out.println("list contents: ");
        while(curr != null){
            T out = curr.getData();
            System.out.println(out.toString());
            curr = curr.getLink();
        }
        System.out.println(" ");
    }
    public void printList(){
        Node2<T> curr = start;
        System.out.println("list contents: ");
        while(curr != null){
            T out = curr.getData();
            System.out.println(out);
            curr = curr.getLink();
        }
        System.out.println(" ");
    }
    public int getSize(){
        return size;
    }
    public Node2 getStart(){
        return start;
    }
    public T getData(int i){
        Node2<T> curr = start;
        for(int j=1; j<i; j++){
            curr = curr.getLink();
        }
        return curr.getData();
    }
}
//Queue2.java: link implementation
class Queue2<T extends Comparable<T>>{
    
    private Node2<T> front;
    private T temp;
    private Node2<T> back;
    private int size;
    
    //constructor
    public Queue2(){
        front = null;
        back = null;
        size = 0;
    }
    
    /*
     public void remove(T y){
     if(size==0){
     return;
     }
     if(front.getData().compareTo(y) == 0){
     front = front.getLink();
     size--;
     return;
     }
     Node2<T> cursor = front;
     Node2<T> cursor2 = front;
     
     while(cursor.getData().compareTo(y) != 0){
     cursor2 = cursor;
     cursor = cursor.getLink();
     }
     if(cursor.getData().compareTo(y) == 0){
     cursor2.setLink(cursor.getLink());
     }
     size --;
     
     if(cursor.getLink() == null){
     back = cursor2;
     cursor2 = null;
     }
     }
     */
    
    public T remove(T y){
        if(front.getData().equals(y) == true){
            temp = front.getData();
            front = front.getLink();
            return temp;
        }
        Node2<T> cursor = front;
        Node2<T> cursor2 = front;
        
        while(cursor.getData().equals(y) != true){
            cursor2 = cursor;
            cursor = cursor.getLink();
        }
        if(cursor.getData().equals(y) == true){
            temp = cursor.getData();
            cursor2.setLink(cursor.getLink());
            return temp;
        }
        if(cursor.getLink() == null){
        temp = back.getData();
        back = cursor2;
        cursor2 = null;
        return temp;
        }
        size --;
        return null;
    }
    
    //member methods
    public void enqueue(T y){
        Node2 newnode = new Node2(y, null);
        assert(newnode != null);
        size ++;
        if(back == null){
            front = newnode;
            back = newnode;
        }
        else{
            back.setLink(newnode);
            back = newnode;
        }
    }
    public T getBackData(){
        if(size == 1){
            return front.getData();
        }
        else if(size>0){
            return back.getData();
        }
        else{
            return null;
        }
    }
    
    public T dequeue(){
        assert(front != null);
        T dq = front.getData();
        Node2 save = front.getLink();
        front = save;
        size --;
        if(size == 0){
            back = null;
        }
        return dq;
    }
    public int getSize(){
        return size;
    }
    public T getFrontData(){
        return front.getData();
    }
    public void printQ(){
        Node2 curr = front;
        if(front == null){
            System.out.println("queue is empty");
            return;
        }
        while(curr != null){
            System.out.println(curr.getData());
            curr = curr.getLink();
        }
        System.out.println("  ");
    }
    // public Node2 getFront()
    // public Node2 getBack()
    // public void setFront(Node2 newfront)
    // public void setBack(Node2 newback)
    public void reverseQ(){ //reverse the order of the elements
        Stack1gen<T> s = new Stack1gen<T>();
        while(size > 0){
            s.push(dequeue());
        }
        while(s.getSize()>0){
            enqueue(s.pop());
        }
    }
    public static void shift(Queue2 A, int n, Queue2 B){
        //A must be nonempty; keep the first n elements of A (n >= 0);
        //shift the remaining elements of A to the rear of B,
        //keeping the same order.
        //dummy code
        A.dequeue();
    }
    //Exercise: Give a new implementation of reverse(), which does not
    //use stacks, enqueue(), or dequeue(), but just uses the shift() method.
}
//Stack1gen.java
//array implementation of stack class
class Stack1gen<T>{
    int MAX = 30; //maximum number of stack entries
    private int top;
    private T[] stack; //array to hold the stack data
    //default constructor
    public Stack1gen(){
        top = MAX; //initialize an empty stack
        stack = (T[]) new Object[MAX];
    }
    //copy constructor
    //  public Stack1(Stack1 s)
    //  {
    //    top = s.top;
    //    for(int i = top; i<=MAX-1; i++)
    //   {
    //      stack[i] = s.stack[i];
    //    }
    //  }
    public void push(T y){  //push data item y onto the stack
        assert(top > 0); //check that there is room on the stack
        top = top -1;
        stack[top] = y;
    }
    public T pop(){ //pop the top item from the stack and return it
        assert(top < MAX); //check that there is data on the stack
        T x = stack[top];
        top = top +1;
        return x;
    }
    public int getSize(){
        return MAX-top;
    }
    public T getTop(){
        assert(top < MAX);
        return stack[top];
    }
    // public void printStack()  //print the contents of the stack, from
    // top to bottom, one item per line, without popping the stack items.
}
class Heap1customer{
    int MAX = 100; //maximum number of heap entries
    private int n; //size of heap
    private Customer[] heap; //array to hold the heap data
    //default constructor
    public Heap1customer(){
        n=0;
        heap = new Customer[MAX];//reserve an array of size MAX
        //for type Customer objects
    }
    public void reheap(int j, int m){
        //j is the root index of the subtree
        //m is the final index of the subtree
        int swap = 0; //signal for a swap
        Customer min; //the smaller of the data values of the children of the current node
        int place; //the array location of the smaller of the children
        do
        {
            if(2*j <= m){ //if current node is not a leaf
                if(2*j+1 > m){ //there is no right child
                    min = heap[2*j]; //the left child
                    place = 2*j;
                }
                else{
                    if(heap[2*j].compareTo(heap[2*j+1]) <= 0){ //compare the children
                        min = heap[2*j];
                        place = 2*j;
                    }
                    else{
                        min = heap[2*j+1];
                        place = 2*j+1;
                    }
                }
                if(min.compareTo(heap[j]) < 0){  //compare smaller child to parent
                    //if smaller child comes before parent, swap
                    Customer temp;
                    temp = heap[j];
                    heap[j]= heap[place];
                    heap[place] = temp;
                    swap = 1;
                    j = place; //redefine the current parent node for the next pass
                }
                else{
                    //no swap needed; tree is already a heap; prepare to exit
                    swap = 0;
                }
            }
        }while((2*j <= m) && (swap ==1)); //exit when tree is a heap
    }
    
    public void create_heap(){
        for(int i = n/2; i >= 1; i = i-1){
            reheap(i,n); //reheap the subtree beginning at node i
        }/*
          System.out.println("heap array:");
          for(int i = 1; i<= n; i++){
          System.out.println(heap[i] + "   ");
          }*/
    }
    
    public void heapsort(){
        System.out.println("  ");
        System.out.println("Heap data in sort order:");
        for(int i = 1; i <= n; i++){
            System.out.println( heap[1]);
            heap[1] = heap[n-i+1];
            reheap(1, n-i);
        }
        System.out.println("  ");
        n=0;
    }
    public int getSize(){
        return n;
    }
    public Customer dequeue(){
        Customer save = heap[1];
        heap[1] = heap[n];
        n = n-1;
        reheap(1,n);
        return save;
    }
    public void enqueue(Customer y){
        n = n + 1;
        heap[n] = y;
        create_heap();
    }
    public Customer getFirst(){
        return heap[1];
    }
}







