import websocket, time, sys, json, robot


class mainApp():
    def __init__(self, host):
        self.robot = robot.Robot()
        ws = websocket.WebSocketApp(
            host,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close
        )
        ws.on_open = self.on_open
        ws.run_forever()

    def on_message(self, ws, message):
        try:
            data = json.loads(str(message).strip())
            if "to" in data and data["to"] == "ROBOT":
                self.moveRobot(data, ws)
        except ValueError:
            print("Ignore:", message)

    def on_error(self, ws, error):
        print(error)

    def on_close(self, ws):
        print("### closed ###")

    def on_open(self, ws):
        print("connected to WSS")

    def is_robot_connected(self):
        if self.robot.isAlive():
            return True
        return False

    def sendMsg(self, ws, taskId, **kwargs):
        status = kwargs.pop('status', 'error')
        message = kwargs.pop('msg', 'no message')
        code = kwargs.pop('code', 500)
        msg = {
            "from": "ROBOT",
            "taskId": taskId,
            "status": status,
            "message": message,
            "code": code
        }
        ws.send(str(msg))

    def moveRobot(self, data, ws):
        taskId = "0"
        if "taskId" in data:
            taskId = data["taskId"]

        if "tasks" not in data:
            self.sendMsg(ws, taskId, msg="Tasks not found", code=7001)
            return

        allowedMoves = ["f", "r", "l", "c","s"]
        hasConnectRequest = False

        if "distance" in data:
            self.robot.updateDistance(data["distance"])

        for task in data["tasks"]:
            if not task in allowedMoves:
                self.sendMsg(ws, taskId, msg="Unknown task " + task, code=7002)
                return

            if task is "c":
                hasConnectRequest = True

        isConnected = False

        if hasConnectRequest:
            if self.is_robot_connected():
                self.sendMsg(ws, taskId, msg="Robot is connected", status="success", code=200)
                isConnected = True
            else:
                if self.robot.connectToEV3():
                    self.sendMsg(ws, taskId, msg="Robot connected", status="success", code=201)
                    isConnected = True
                else:
                    self.sendMsg(ws, taskId, msg="Robot is offline", code=7003)
                    return
        else:
            if self.is_robot_connected():
                isConnected = True

        if not isConnected:
            self.sendMsg(ws, taskId, msg="Robot is offline", code=7004)
            return

        if not self.robot.allowedColor():
            self.sendMsg(ws, taskId, msg="Color error", code=7010)
            return

        for task in data["tasks"]:
            if task is "c":
                continue
                
            self.sendMsg(ws, taskId, msg=task, code=222, status="success")
            if task is "s":
                time.sleep(1)
            else:
                print("do:",task)
                time.sleep(0.25)
                self.robot.taskManager(task)


if __name__ == "__main__":
    websocket.enableTrace(False)
    if len(sys.argv) < 2:
        host = "ws://localhost:8886"
    else:
        host = sys.argv[1]

    mainApp(host)
