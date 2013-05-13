package org.ozb.utils.pubsub

/**
 * Simple Pub/Sub implementation inspired from http://jim-mcbeath.blogspot.com/2009/10/simple-publishsubscribe-example-in.html
 * 
 * why not use the sandard scala.collection.mutable.Publisher ? => subscribers are not closures...
 */

trait Publisher[E] {
	type Sub = (E) => Any
	private var subscribers: List[Sub] = Nil
	// By using lock.synchronized rather than this.synchronized we reduce
    // the scope of our lock from the extending object (which might be
    // mixing us in with other classes) to just this trait.
	private object lock
	
	 /** True if the subscriber is already in our list. */
    def isSubscribed(subscriber: Sub) = {
        val subs = lock.synchronized { subscribers }
        subs.exists(_ == subscriber)
    }

    /** Add a subscriber to our list if it is not already there. */
    def subscribe(subscriber: Sub) = lock.synchronized {
        if (! isSubscribed(subscriber))
            subscribers = subscriber :: subscribers
    }

    /** Remove a subscriber from our list.  If not in the list, ignored. */
    def unsubscribe(subscriber: Sub) = lock.synchronized {
        subscribers = subscribers.filter(_ != subscriber)
    }

    /** Publish an event to all subscribers on the list. */
    def publish(event: E) = {
        val subs = lock.synchronized { subscribers }
        subs.foreach(_.apply(event))
    }
}