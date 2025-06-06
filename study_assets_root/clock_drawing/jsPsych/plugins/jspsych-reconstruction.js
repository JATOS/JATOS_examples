/**
 * jspsych-reconstruction
 * a jspsych plugin for a reconstruction task where the subject recreates
 * a stimulus from memory
 *
 * Josh de Leeuw
 *
 * documentation: docs.jspsych.org
 *
 */


jsPsych.plugins['reconstruction'] = (function() {

  var plugin = {};

  plugin.info = {
    name: 'reconstruction',
    description: '',
    parameters: {
      stim_function: {
        type: [jsPsych.plugins.parameterType.FUNCTION],
        default: undefined,
        no_function: false,
        description: ''
      },
      starting_value: {
        type: [jsPsych.plugins.parameterType.FLOAT],
        default: 0.5,
        no_function: false,
        description: ''
      },
      step_size: {
        type: [jsPsych.plugins.parameterType.FLOAT],
        default: 0.05,
        no_function: false,
        description: ''
      },
      key_increase: {
        type: [jsPsych.plugins.parameterType.KEYCODE],
        default: 'h',
        no_function: false,
        description: ''
      },
      key_decrease: {
        type: [jsPsych.plugins.parameterType.KEYCODE],
        default: 'g',
        no_function: false,
        description: ''
      },
      button_label: {
        type: [jsPsych.plugins.parameterType.STRING],
        default: 'Next',
        no_function: false,
        description: ''
      }
    }
  }

  plugin.trial = function(display_element, trial) {

    // default parameter values
    trial.starting_value = (typeof trial.starting_value == 'undefined') ? 0.5 : trial.starting_value;
    trial.step_size = trial.step_size || 0.05;
    trial.key_increase = trial.key_increase || 'h';
    trial.key_decrease = trial.key_decrease || 'g';
    trial.button_label = typeof trial.button_label === 'undefined' ? 'Next' : trial.button_label;

    // if any trial variables are functions
    // this evaluates the function and replaces
    // it with the output of the function
    trial = jsPsych.pluginAPI.evaluateFunctionParameters(trial, ['stim_function']);

    // current param level
    var param = trial.starting_value;

    // set-up key listeners
    var after_response = function(info) {

      //console.log('fire');

      var key_i = (typeof trial.key_increase == 'string') ? jsPsych.pluginAPI.convertKeyCharacterToKeyCode(trial.key_increase) : trial.key_increase;
      var key_d = (typeof trial.key_decrease == 'string') ? jsPsych.pluginAPI.convertKeyCharacterToKeyCode(trial.key_decrease) : trial.key_decrease;

      // get new param value
      if (info.key == key_i) {
        param = param + trial.step_size;
      } else if (info.key == key_d) {
        param = param - trial.step_size;
      }
      param = Math.max(Math.min(1, param), 0);

      // refresh the display
      draw(param);
    }

    // listen for responses
    var key_listener = jsPsych.pluginAPI.getKeyboardResponse({
      callback_function: after_response,
      valid_responses: [trial.key_increase, trial.key_decrease],
      rt_method: 'date',
      persist: true,
      allow_held_key: true
    });
    // draw first iteration
    draw(param);

    function draw(param) {

      //console.log(param);

      display_element.innerHTML = '<div id="jspsych-reconstruction-stim-container">'+trial.stim_function(param)+'</div>';

      // add submit button
      display_element.innerHTML += '<button id="jspsych-reconstruction-next" class="jspsych-btn jspsych-reconstruction">'+trial.button_label+'</button>';

      display_element.querySelector('#jspsych-reconstruction-next').addEventListener('click', endTrial);
    }

    function endTrial() {
      // measure response time
      var endTime = (new Date()).getTime();
      var response_time = endTime - startTime;

      // save data
      var trial_data = {
        "rt": response_time,
        "final_value": param,
        "start_value": trial.starting_value
      };

      display_element.innerHTML = '';

      // next trial
      jsPsych.finishTrial(trial_data);
    }

    var startTime = (new Date()).getTime();

  };

  return plugin;
})();
