/* matrix summation using pthreads

   features: uses a barrier; the Worker[0] computes
             the total sum from partial sums computed by Workers
             and prints the total sum to the standard output

   usage under Linux:
     gcc matrixSum.c -lpthread
     a.out size numWorkers

*/

#ifndef _REENTRANT
#define _REENTRANT
#endif

#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <sys/time.h>

#define MAXSIZE 1000  /* maximum matrix size */
#define MAXWORKERS 2   /* maximum number of workers */
#define MATRIX_LIMIT 10000 /* maximum possible value of the matrix elements*/

pthread_mutex_t barrier;  /* mutex lock for the barrier */
pthread_cond_t go;        /* condition variable for leaving */
int numWorkers;           /* number of workers */
int numArrived = 0;       /* number who have arrived */

struct extreme {
    int value;
    int pos_i, pos_j;
};

/* a reusable counter barrier */
void Barrier() {
    pthread_mutex_lock(&barrier);
    numArrived++;
    if (numArrived == numWorkers) {
        numArrived = 0;
        pthread_cond_broadcast(&go);
    } else
        pthread_cond_wait(&go, &barrier);
    pthread_mutex_unlock(&barrier);
}

/* timer */
double read_timer() {
    static bool initialized = false;
    static struct timeval start;
    struct timeval end;
    if(!initialized) {
        gettimeofday(&start, NULL);
        initialized = true;
    }
    gettimeofday(&end, NULL);
    return (end.tv_sec - start.tv_sec) + 1.0e-6 * (end.tv_usec - start.tv_usec);
}

double start_time, end_time; /* start and end times */
int size, stripSize;  /* assume size is multiple of numWorkers */
int sums[MAXWORKERS]; /* partial sums */
struct extreme mins[MAXWORKERS]; /* minimizer for each worker */
struct extreme maxs[MAXWORKERS]; /* maximizer for each worker */
int matrix[MAXSIZE][MAXSIZE]; /* matrix */

void *Worker(void *);

struct extreme min(struct extreme a, struct extreme b) {
    return a.value < b.value ? a : b;
}

struct extreme max(struct extreme a, struct extreme b) {
    return a.value > b.value ? a : b;
}

int sumElementsInStrip(int first, int last) {
    int total = 0;
    for (int i = first; i <= last; i++)
        for (int j = 0; j < size; j++)
            total += matrix[i][j];

    return total;
}

/* Returns the value and position of the minimum (mode = 0) or maximum (mode = 1) of matrix*/
struct extreme obtainExtremeInStrip(int first, int last, int mode) {

    if(mode == 0) { // Minimum
        struct extreme ext = {.value = MATRIX_LIMIT, .pos_i = -1, .pos_j = -1};

        for (int i = first; i <= last; i++) {
            for (int j = 0; j < size; j++) {
                if(matrix[i][j] < ext.value) {
                    ext.value = matrix[i][j];
                    ext.pos_i = i; ext.pos_j = j;
                }
            }
        }

        return ext;
    }

    else { // Maximum
        struct extreme ext = {.value = -1, .pos_i = -1, .pos_j = -1};

        for (int i = first; i <= last; i++) {
            for (int j = 0; j < size; j++) {
                if(matrix[i][j] > ext.value) {
                    ext.value = matrix[i][j];
                    ext.pos_i = i; ext.pos_j = j;
                }
            }
        }

        return ext;
    }
}

/* read command line, initialize, and create threads */
int main(int argc, char *argv[]) {

    srand(time(0));

    int i, j;
    long l; /* use long in case of a 64-bit system */
    pthread_attr_t attr;
    pthread_t workerid[MAXWORKERS];

    /* set global thread attributes */
    pthread_attr_init(&attr);
    // Thread competes for resources with all the other threads of the system.
    pthread_attr_setscope(&attr, PTHREAD_SCOPE_SYSTEM);

    /* initialize mutex and condition variable */
    pthread_mutex_init(&barrier, NULL);
    pthread_cond_init(&go, NULL);

    /* read command line args if any */
    size = (argc > 1)? atoi(argv[1]) : MAXSIZE;
    numWorkers = (argc > 2)? atoi(argv[2]) : MAXWORKERS;
    if (size > MAXSIZE) size = MAXSIZE;
    if (numWorkers > MAXWORKERS) numWorkers = MAXWORKERS;
    stripSize = size/numWorkers;

    /* initialize the matrix */
    for (i = 0; i < size; i++) {
        for (j = 0; j < size; j++) {
            matrix[i][j] = 1;//rand() % MATRIX_LIMIT;
        }
    }

    /* initialize mins and maxs */
    for (i = 0; i < numWorkers; i++) {
        struct extreme e = {.value = MATRIX_LIMIT, .pos_i = -1, .pos_j = -1};
        mins[i] = e;
        e.value = -1;
        maxs[i] = e;
    }

    /* print the matrix */
#ifdef DEBUG
    for (i = 0; i < size; i++) {
	  printf("[ ");
	  for (j = 0; j < size; j++) {
	    printf(" %d", matrix[i][j]);
	  }
	  printf(" ]\n");
  }
#endif

    /* do the parallel work: create the workers */
    start_time = read_timer();
    for (l = 0; l < numWorkers; l++)
        pthread_create(&workerid[l], &attr, Worker, (void *) l);

    pthread_exit(NULL);
}


/* Each worker sums the values in one strip of the matrix.
   After a barrier, worker(0) computes and prints results */
void *Worker(void *arg) {
    long myid = (long) arg;
    int total = 0, first, last;
    struct extreme minimum = {.value = MATRIX_LIMIT, .pos_i = -1, .pos_j = -1};
    struct extreme maximum = {.value = -1, .pos_i = -1, .pos_j = -1};

#ifdef DEBUG
    printf("worker %d (pthread id %d) has started\n", myid, pthread_self());
#endif

    /* determine first and last rows of my strip */
    first = myid * stripSize;
    last = (myid == numWorkers - 1) ? (size - 1) : (first + stripSize - 1);

    // Sum values in my strip
    sums[myid] = sumElementsInStrip(first, last);

    // Obtain min and max
    mins[myid] = obtainExtremeInStrip(first, last, 0);
    maxs[myid] = obtainExtremeInStrip(first, last, 1);

    Barrier();
    if (myid == 0) {

        // Obtain total sum, min and max.
        total = 0;
        for (int i = 0; i < numWorkers; i++) {
            total += sums[i];
            minimum = min(minimum, mins[i]);
            maximum = max(maximum, maxs[i]);
        }

        /* get end time */
        end_time = read_timer();

        /* print results */
        printf("The total sum is %d\n", total);
        printf("The overall minimum is %d at position (%d, %d)\n", minimum.value, minimum.pos_i, minimum.pos_j);
        printf("The overall maximum is %d at position (%d, %d)\n", maximum.value, maximum.pos_i, maximum.pos_j);
        printf("The execution time is %g sec\n", end_time - start_time);
    }

    return NULL;
}
