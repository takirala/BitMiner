import java.security.MessageDigest
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import com.typesafe.config._
import akka.actor._
import akka.actor._
import akka.actor.ActorDSL._
import akka.actor.ActorSelection.toScala

/** 
  * @author
  * Tarun Gupta Akirala
  */

object Project1 {

  val randStringLength = 4;
  var serverIP = "localhost";
  var port = 2552

  sealed trait Seal
  case class doTask() extends Seal
  case class GetTask() extends Seal
  case class Task(length: Int, leadingZ: Int, gatorId: String) extends Seal
  case class BitCoin(src: String, value: String) extends Seal
  //case class WorkerDuration(counter: Double, duration: Long)

  var clientConf = """
    akka {
  #log-config-on-start = on
  stdout-loglevel = "INFO"
  loglevel = "INFO"
  actor {
      provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
    enabled-transports = ["akka.remote.netty.tcp"]
    log-sent-messages = on
    log-received-messages = on
    netty.tcp {
          hostxname = !serverIP!
          port = 0
      }
    }  
  }"""

  val masterConf: Config = ConfigFactory.parseString("""
    akka {
  loglevel = "INFO"

  actor {
    provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
    enabled-transports = ["akka.remote.netty.tcp"]
    netty.tcp {
      hostxname = localhost
      port = 2552
    }
    log-sent-messages = on
    log-received-messages = on
  }
  }""")

  var arr = new Array[Char](26)
  var arrCounter = -1
  var counter = 0
  var bestBitCoin = Unit.toString()
  var highestZeroesSoFar = 0

  var leadingZ: Int = 2
  def main(args: Array[String]): Unit = {

    for (i <- 0 to 25) arr(i) = (96 + 1 + i).toChar

    if (args.length > 0) {
      if (args(0).equals("localhost") || args(0).contains('.')) {
        // Spawn client as IP is passed in the argument.
        println("Booting as client")
        if (args(0).contains(':')) {
          var address = args(0).split(':')
          serverIP = address(0)
          port = address(1).toInt
        } else {
          serverIP = args(0);
        }
        clientConf = clientConf.replace("!serverIP!", serverIP)
        spawnClient(args(0));
      } else {
        println("Booting as master")
        try {
          leadingZ = args(0).toInt;
        } catch {
          case t: Throwable => 0
        }
        // Spawn the master thread.
        spawnMaster(leadingZ)
      }
    } else {
      println("No arguments found, exiting!!")
      sys.exit()
    }
  }

  def spawnMaster(leadingZ: Int): Unit = {
    val system = ActorSystem("MasterSystem", masterConf)
    val remoteActor = system.actorOf(Props(new MasterActor(system)), name = "RemoteActor")
    val remoteActorWorker = system.actorOf(Props(new MasterActorWorker(remoteActor)), name = "RemoteActorWorker")
    //remoteActor ! "The RemoteActor is alive"
    remoteActorWorker ! doTask()
    println("Server ready")
  }

  def spawnClient(serverIP: String): Unit = {
    implicit val system = ActorSystem("ClientSystem", ConfigFactory.parseString(clientConf))
    val localactor = system.actorOf(Props(new ClientActor(system)), name = "LocalActor")    
    //println("system selected is " + localactor)
    localactor ! GetTask()
  }

  class ClientActor(system: ActorSystem) extends Actor {
    // create the remote actor
    val remote = context.actorSelection("akka.tcp://MasterSystem@" + serverIP + ":" + port + "/user/RemoteActor")
    var counter = 0

    def receive = {
      case y: GetTask =>
        //println("asking server for some work")
        remote ! GetTask()
      case x: Task => {
        if(x.leadingZ == -1) {
          system.shutdown()
        }else {
          //println("got some work from the server")
          var bitcoin = doJob(x.length, x.gatorId, x.leadingZ, sender);
          if (bitcoin == -1) {
            remote ! GetTask()
          }
        }
        
      }
    }
  }

  class MasterActorWorker(masterRef: ActorRef) extends Actor {
    def receive = {
      case doTask() =>
        masterRef ! GetTask()
      case x: Task => {
        var result = doJob(x.length, x.gatorId, x.leadingZ, masterRef);
        if (result == -1) self ! doTask()
      }
    }
  }

  class MasterActor(system: ActorSystem) extends Actor {

    def receive = {
      case GetTask() =>
        //println("get task called by " + sender)
        arrCounter = arrCounter + 1
        if (arrCounter >= 26) {
          println("Best bit coin so far : " + bestBitCoin)
          system.shutdown()
          sender ! Task(randStringLength, -1, "takirala;" + arr(arrCounter))
        }else {
          sender ! Task(randStringLength, leadingZ, "takirala;" + arr(arrCounter))
        }
        
      case b: BitCoin => {
        val matched = ("^[0]*").r.findFirstMatchIn(b.value)
        matched match {
          case Some(m) => {
            if (m.group(0).length > highestZeroesSoFar) {
              highestZeroesSoFar = m.group(0).length
              bestBitCoin = b.src
              println("Best bit coin so far " + bestBitCoin)
            }
          }
          case None =>
        }
        println(b.src + "\t\t" + b.value)        
      }
    }
  }

  def doJob(length: Int, prefix: String, leadingZ: Int, sender: ActorRef): Int = {

    //println("Job called for " + sender)
    var hasMoreChars = false
    val Patt = ("^([0]{" + leadingZ.toString() + "}[a-f0-9]*)+$").r
    var hexString = Unit.toString()

    for (i <- 1 to length) {
      var start: Long = System.currentTimeMillis
      val maxCounter: Double = Math.pow(26, i)
      try {
        while (!hasMoreChars) {          
          val x = randomString(i, prefix, maxCounter)
          val hash = MessageDigest.getInstance("SHA-256").digest(x.getBytes("UTF-8"))
          hexString = hash.map("%02X" format _).mkString
          //println("String passed : " + x + " \tDigest : " + hexString)
          hexString = hexString.toString().toLowerCase()
          if (Patt.pattern.matcher(hexString).matches()) {
            sender ! BitCoin(x, hexString)
            //println("Sent bitcoin " + hexString)
          }
        }
      } catch {
        case t: Exception => {
          //println("Length range over " + i)
          counter = 0          
        }
      }
    }
    //println("Hurrayy bitcoin is : " + hexString)
    -1
  }

  def randomString(length: Int, gatorId: String, maxCounter: Double) = {

    if (counter < maxCounter) {
      var result = ""
      var i = counter
      for (j <- 1 to length) {
        result += (('a' + (i % 26))).toChar;
        i = i / 26;
      }
      counter = counter + 1
      gatorId + result.toString
    } else {
      throw new Exception("all characters in given range executed")
    }
  }

}