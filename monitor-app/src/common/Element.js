/*
 * Copyright 2021 crunchycookie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemText from "@material-ui/core/ListItemText";
import CheckCircleIcon from '@material-ui/icons/CheckCircle';
import PauseCircleOutlineIcon from '@material-ui/icons/PauseCircleOutline';
import CircularProgress from '@material-ui/core/CircularProgress';

import {
  Card,
  CardContent,
  Divider,
  Grid,
  Paper,
  Typography
} from "@material-ui/core";

function Element(props) {
  return (
      <div className="orion-element">
        <Grid container spacing={3}>
          <Card className="card" style={{overflow: 'auto'}}>
            <CardContent style={{overflow: 'auto'}}>
              <Paper style={{maxHeight: 700, overflow: 'auto'}}>
                <List>
                  {
                    props.tasks.map(task => (
                        <Paper variant="outlined">
                          <ListItem>
                            {task.state === "successful" && (
                                <CheckCircleIcon/>)}
                            {(task.state === "inprogress"
                                && !props.elemantName.includes(
                                    "Priority Queue")) && (<CircularProgress/>)}
                            {(task.state === "inprogress"
                                && props.elemantName.includes("Priority Queue"))
                            && (<PauseCircleOutlineIcon/>)}
                            <ListItemText
                                primary={task.state.toUpperCase()}
                                secondary={task.id}
                            />
                          </ListItem>
                        </Paper>
                    ))
                  }
                </List>
              </Paper>
              <Divider variant="middle"/>
              <Typography variant="subtitle1" align="center">
                {props.elemantName}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </div>
  );
}

export default Element;