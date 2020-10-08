#!/usr/bin/env python3
"""
Class handling simple commands for EV3
"""
import ev3, ev3_vehicle, struct, time, math, threading


class Robot():
    def __init__(self, **kwargs):
        self.diameter = kwargs.pop('diameter', 43.2)  # mm
        self.radius = self.diameter / 2  # mm
        self.circuit = 2 * math.pi * self.radius  # mm
        self.degree = self.circuit / 360  # mm
        self.distance = kwargs.pop('distance', self.circuit)
        self.distanceDegrees = math.ceil(self.distance / self.degree)
        self.speed = kwargs.pop('speed', 40)
        self.verbosity = kwargs.pop('verbosity', 0)
        self.host = kwargs.pop('host', '00:16:53:56:06:B2')
        self.ev3 = False
        self.connected = False
        self.rotateRatio = 60 / 90
        self.connectToEV3()
        self.angle = 90
        self.shouldGo = False
        self.color = None

    def updateDistance(self, distance):
        self.distance = distance
        self.distanceDegrees = math.ceil(self.distance / self.degree)

    def connectToEV3(self):
        print('connecting to EV3...')
        try:
            self.ev3 = ev3_vehicle.TwoWheelVehicle(
                self.radius * 1000,
                self.circuit * 1000,
                protocol=ev3.BLUETOOTH,
                host=self.host
            )
            self.ev3.verbosity = self.verbosity
            self.connected = True
            print("EV3 connected")
            return True
        except:
            self.connected = False
            print("EV3 connection fail")
            return False

    def readPosition(self):
        ops = b''.join([
            ev3.opInput_Device,
            ev3.READY_SI,
            ev3.LCX(0),  # LAYER
            ev3.LCX(16),  # MOTOR_PORT_A
            ev3.LCX(7),  # TYPE
            ev3.LCX(0),  # MODE
            ev3.LCX(1),  # VALUES
            ev3.GVX(0),  # VALUE1
            ev3.opInput_Device,
            ev3.READY_RAW,
            ev3.LCX(0),  # LAYER
            ev3.LCX(19),  # MOTOR_PORT_D
            ev3.LCX(7),  # TYPE
            ev3.LCX(0),  # MODE
            ev3.LCX(1),  # VALUES
            ev3.GVX(4)  # VALUE1
        ])

        reply = self.ev3.send_direct_cmd(ops, global_mem=8)
        (pos_a, pos_d) = struct.unpack('<fi', reply[5:])
        return {"a": pos_a, "d": pos_d}

    def move(self):

        data = self.readPosition()

        start_a = data["a"]
        start_d = data["d"]
        #print("start")
        self.ev3.drive_straight(self.speed, self.distance * 1000)
        self.ev3.stop(brake=True)
        data2 = self.readPosition()

        diffA = data2["a"] - start_a
        diffB = data2["d"] - start_d
        left = ((diffA + diffB) / 2) - self.distanceDegrees

        #print("stop 1, left:", left)

        speed = 20
        times20 = 4
        i = 0

        time.sleep(0.5)

        try:
            while True:
                if abs(left) <= 3 or self.shouldGo is False:
                    break

                if left > 0:
                    speed *= -1

                self.ev3.drive_straight(speed, abs(math.ceil(left * self.degree)))
                self.ev3.stop(brake=True)
                currentPos = self.readPosition()
                diffA = currentPos["a"] - start_a
                diffB = currentPos["d"] - start_d

                left = ((diffA + diffB) / 2) - self.distanceDegrees
                if i > times20:
                    speed = 5
                else:
                    speed = 15
                i += 1
        except:
            pass

        #print("======== DONE ============")
        #print("left:", left)
        #print("attempts:", i)
        #print("port_a:", diffA)
        #print("port_d:", diffB)
        self.stop()

    def turnRight(self):
        self.ev3.drive_turn(20, 0, self.angle)
        self.stop()

    def turnLeft(self):
        self.ev3.drive_turn(20, 0, self.angle, right_turn=True)
        self.stop()

    def stop(self):
        #print("stop!")
        self.shouldGo = False
        self.ev3.stop(brake=True)
        time.sleep(0.1)
        self.ev3.stop()

    def allowedColor(self):
        ops = b''.join([
            ev3.opInput_Device,
            ev3.READY_SI,
            ev3.LCX(0),
            ev3.LCX(1),
            ev3.LCX(29),
            ev3.LCX(2),
            ev3.LCX(1),
            ev3.GVX(0)
        ])
        
        reply = self.ev3.send_direct_cmd(ops, global_mem=8)
        (c, tmp) = struct.unpack('<fi', reply[5:])
        self.color = int(c)
        
        if self.color is 0 or self.color is 6:
            self.stop()
            return False

        #self.shouldGo = True
        return True

    def check(self):
        t = threading.currentThread()
        while getattr(t, "do_run", True):
            #print("A")
            if not self.allowedColor():
                self.shouldGo = False
                break

    def taskManager(self, task, **kwargs):
        if self.allowedColor():
                self.shouldGo = True

        if task not in {'f', 'r', 'l'}:
            return False

        if not self.shouldGo:
            print("wrong color",self.color)
            return False

        if not self.isAlive:
            self.connectToEV3()

        self.angle = kwargs.pop('angle', 90) * self.rotateRatio
        self.distance = kwargs.pop('distance', self.distance)

        a = None
        if task is 'f':
            a = threading.Thread(target=self.move)
        elif task is 'r':
            a = threading.Thread(target=self.turnRight)
        elif task is 'l':
            a = threading.Thread(target=self.turnLeft)
        else:
            return False

        b = threading.Thread(target=self.check)

        a.start()
        b.start()

        #a.join()
        #b.join()
        
        while self.shouldGo:
            pass

        b.do_run = False

        if not self.allowedColor():
            return False

        #del self.ev3
        return True

    def isAlive(self):
        if not self.connected:
            return False

        try:
            self.readPosition()
            return True
        except:
            return False


if __name__ == "__main__":
    x = Robot()
    while True:
        task = input('Podaj zadanie [f/r/l/q]: ')
        if task is 'q':
            break
        x.taskManager(task)
