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
import {
  Card,
  CardContent,
  Divider,
  Grid,
  Paper,
  Typography
} from "@material-ui/core";

function Element(props) {
  console.log(props.tasks);
  return (
      <div className="element">
        <Grid container alignItems="center">
          <Card className="card">
            <CardContent>
              <List>
                {
                  props.tasks.map(task => (
                      <Paper variant="outlined">
                        <ListItem>
                          <ListItemText
                              primary="Submitted Task"
                              secondary={"Task ID: " + task.id}
                          />
                        </ListItem>
                      </Paper>
                  ))
                }
              </List>
              <Divider light/>
              <Typography variant="h6" component="subtitle1">
                {props.elemantName}
              </Typography>
            </CardContent>
          </Card>

        </Grid>
      </div>
  );
}

export default Element;