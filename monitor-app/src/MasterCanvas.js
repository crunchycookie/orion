import './App.css';
import {makeStyles, Paper} from "@material-ui/core";
import Element from "./common/Element";
import {useEffect, useState} from "react";

const useStyles = makeStyles((theme) => ({
  root: {
    display: 'flex',
    flexWrap: 'wrap',
    '& > *': {
      margin: theme.spacing(1),
      width: theme.spacing(16),
      height: theme.spacing(16),
    },
  },
}));

function MasterCanvas() {

  const axios = require('axios').default;

  const [workerPool, setWorkerPool] = useState([]);
  const [queue, setQueue] = useState([]);
  const [centralStore, setCentralStore] = useState([]);

  const classes = useStyles();

  useEffect(() => {
    axios.get('http://localhost:8080/orion/v0.1/get-state')
    .then(function (response) {
      setQueue(response.data.priorityQueue);
      setCentralStore(response.data.centralStore);
      setWorkerPool(response.data.workerPool);
    })
    .catch(function (error) {
      // handle error
      console.log(error);
    })
    .then(function () {
      // always executed
    });
  });

  return (
      <div className={classes.root}>
        <Paper elevation={0}>
          <Element
              tasks={[
                {id: 123},
                {id: 456}
              ]}
              elemantName="Central Store"
          />
        </Paper>
        <Paper elevation={0}>
          <Element
              tasks={[
                {id: 123},
                {id: 456}
              ]}
              elemantName="Priority Queue"
          />
        </Paper>
        <Paper elevation={0} >
          <Element
              tasks={[
                {id: 123},
                {id: 456}
              ]}
              elemantName="Worker Pool"
          />
        </Paper>
      </div>
  );
}

export default MasterCanvas;
