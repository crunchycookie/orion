import {Paper} from "@material-ui/core";
import Element from "./common/Element";
import {useEffect, useState} from "react";
import "./css/MasterCanvas.css";
import ArrowForwardIcon from '@material-ui/icons/ArrowForward';

function MasterCanvas() {

  const axios = require('axios').default;

  const [workerPool, setWorkerPool] = useState([]);
  const [queue, setQueue] = useState([]);
  const [centralStore, setCentralStore] = useState([]);

  const getStats = async () => {
    axios.get('http://localhost:8080/orion/v0.1/get-state')
    .then(function (response) {
      setQueue(response.data.priorityQueue);
      setCentralStore(response.data.centralStore);
      setWorkerPool(response.data.workerPool);
    })
    .catch(function (error) {
      // handle error
    })
    .then(function () {
      // always executed
    });
  }

  useEffect(() => {
    getStats();

    const interval = setInterval(() => {
      getStats()
    }, 1000)

    return () => clearInterval(interval)
  }, []);

  return (
      <div className="master-canvas">
        <Paper elevation={0} className="element">
          <Element
              tasks={queue.map(task => {
                return {id: task.taskId, state: task.status}
              })}
              elemantName="Priority Queue"
          />
        </Paper>
        <ArrowForwardIcon fontSize="large"/>
        <Paper elevation={0} className="element">
          <Element
              tasks={workerPool.map(task => {
                return {id: task.taskId, state: task.status}
              })}
              elemantName="Worker Pool"
          />
        </Paper>
        <ArrowForwardIcon fontSize="large"/>
        <Paper elevation={0} className="element">
          <Element
              tasks={centralStore.map(task => {
                return {id: task.taskId, state: task.status}
              })}
              elemantName="Central Store"
          />
        </Paper>
      </div>
  );
}

export default MasterCanvas;
