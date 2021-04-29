var options;

function isValidUrl(string) {
    var pattern = new RegExp('^(https?:\\/\\/)?'+ // protocol
        '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|'+ // domain name
        '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
        '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // port and path
        '(\\?[;&a-z\\d%_.~+=-]*)?'+ // query string
        '(\\#[-a-z\\d_]*)?$','i'); // fragment locator
    return !!pattern.test(string);
}

(new (function Home__dashboard_user() {

    var _this = home__dashboard = this;

    // user role init code
    this.init_user = function (_translation, _options) {
        translation = _translation;
        options = _options;

        feedback_anonymous();
        if (options.result.first_connect) {
            if (options.result.tutorial_step == 1) {
                $("#modal_dialog").html(options.result.welcome_html);
                $("#modal_dialog").modal('show');
                init_freetrial_tutorial_pagination();
                $("#modal_dialog #confirm").unbind('click').click(function () {
                    $("#modal_dialog").modal('hide');
                    show_popup_freetrial1(options);
                });
            } else if (options.result.tutorial_step < 3) {
                show_popup_freetrial1(options);
            } else {
                show_popup_freetrial2(options);
            }
        }
        if (options.register_class) {
            register(options.register_class.class_id, options.register_class.class_date);
        }

        main_user_js.init_wo();

        $(document).ready(function () {
            $('.popover').on('shown.bs.popover', function () {
                $('.popover .arrow').css('background', 'black')
            })
        });

        $(document).on('click', '.table-responsive.table-calender.coach-table',
            function() {
                $(".popovers").popover('destroy');
            }
        );

        var timeouts = [];
        $(document).on(
            {
                mouseenter: function () {

                    for (var i = 0; i < (timeouts.length); i++) {
                        clearTimeout(timeouts[i]);
                    }

                    var that = $(this).parent();
                    setTimeout(function() {
                        $(".popovers").popover('destroy');
                    }, 500);

                    timeouts.push(setTimeout(function () {

                        var glob = $(that);
                        var id = $(that).attr('id');
                        var str = id.split(",");

                        $("#attending").html('');

                        $(".table-chk").css("z-index", "1");
                        $(that).css("z-index", "10");
                        $("#navlinks").css('display', 'block');


                        popuplist(id, glob);

                        $.ajax({
                            type: "POST",
                            url: "ajax/userdata/class_attending",
                            data: "id=" + str[0] + "&dt=" + str[1],
                            success: function (data) {
                                $('#attending').html(data);
                            }
                        });
                        $(that).css("z-index", "10");
                    }, 550));
                },
                mouseleave: function () {
                    isstopped = true;
                    for (var i = 0; i < (timeouts.length - 1); i++) {
                        clearTimeout(timeouts[i]);
                    }
                    // var that = $(this).parent();

                    // var id = $(that).attr('id');
                    // var str = id.split(",");

                },
            }
            , '.table-chk p');

        $(document).on('click', '.table-chk', function () {
            var id = $(this).data('id');
            var date = $(this).data('date');
            if ($(this).data('deleted')) return;
            $.ajax({
                type: "POST",
                dataType: "json",
                url: "ajax/userdata/get_meeting_url",
                data: {
                    class_id : id,
                    class_date: date,
                },
                success: function (data) {
                    if(!data.error) {
                        if(data.meeting_url) {
                            if(isValidUrl(data.meeting_url)) {
                                var url = data.meeting_url.match(/^https?:/) ? data.meeting_url : '//' + data.meeting_url;
                                window.open(url, '_blank');
                            } else {
                                data.message = data.meeting_url;
                                modal_alert(data);
                            }
                        }
                    } else {
                        modal_alert(data);
                    }
                }
            });
        });

        $(function () {
            var sync_calendar_notification = options.result.sync_calendar_notification || "";
            var dialog_el = $("#modal_dialog");
            if (sync_calendar_notification.length > 0) {
                dialog_el.html(sync_calendar_notification);
                dialog_el.modal('show');
                dialog_el.find('#confirm').click(function () {
                    location.assign('/user/users/edit_user?id=5&help=1#user_sync_calendar');
                });
            }
        });
    };

})());


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// user functions
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

function register(class_id, class_date) {
    $.ajax({
        type: "POST",
        url: "ajax/classinfo/register_class",
        data: {id: class_id, dt: class_date},
        dataType: 'json',
        success: function (data) {
            if (!data.error) {
                $('#book_confirm_modal').html(data.html).modal('show');
                init_fake_counter();
                $('#book_confirm_modal button.close').unbind('click').click( function () {
                    $('#class_' + class_id + "_" + class_date).prop('checked', false);
                });
                $('#book_confirm_modal .btn.btn-cancel').unbind('click').click(function() {
                    $('#class_' + class_id + "_" + class_date).prop('checked', false);
                });

                booking_confirm(class_id, class_date);
            } else {
                $('#class_' + class_id + "_" + class_date).prop('checked', false);
                if(data.code === 1) {
                    register_popup.show_register_popup(class_id, class_date, data.dialog_message);
                } else {
                    modal_alert(data, false)
                }
            }
        }
    });
}

function unregister_waiting(class_id, class_date)
{
    var str = class_id+'_'+class_date;
    $('#confirm').attr('disabled', false);
    $('#confirm3').attr('disabled', false);

    $('#title3').html(translation.cancel_registration);

    $('#myModal_unreg_waiting').modal({backdrop: 'static', keyboard: false});
    $('#confirm3').unbind('click').click(function () {
            $('#confirm3').attr('disabled', true);
            $.ajax({
                type: 'POST',
                data: {id: class_id, dt: class_date},
                url: 'ajax/classinfo/unregister_class',
                dataType: 'json',
                success: function (response) {
                    $('#myModal_unreg_waiting').modal('hide');

                    if (!response.error) {
                        move_to_period($('#default_date').val());
                    } else {
                        $('#bookings_data').html(response.message);
                        $('#bookings').modal('show');
                    }

                    my_schedule();
                }
            });
        });
}

function booking_confirm(class_id, class_date) {
    $('#confirm').unbind('click').click(function (e) {
        e.preventDefault();
        $('#confirm').attr('disabled', true);
        var count = $('#user-book-count').val();
        var left = $('#user-book-count').data('left');

        var callback = function()
        {
            var timeFormat = 'HH:mm';
            var classStartEndTime = $('#' + class_id + '\\,' + class_date + ' span.class_time').text();
            var startTime = classStartEndTime.substring(0, timeFormat.length);

            $.ajax({
                type: 'POST',
                data: {
                    id: class_id,
                    dt: class_date,
                    startDateTime: class_date + 'T' + startTime + ':00',
                    timeZoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
                    contract_id: $("#book_confirm_modal #contract_id").val(),
                    present_number: count,
                    question_1: $('#book_confirm_modal input[name="question_1"]:checked').val(),
                    question_2: $('#book_confirm_modal input[name="question_2"]:checked').val(),
                    question_3: $('#book_confirm_modal input[name="question_3"]:checked').val(),
                },
                url: 'ajax/classinfo/booking_confirm',
                dataType: 'json',
                success: function (response) {
                    $('#book_confirm_modal').modal('hide');

                    if (response.error) {
                        modal_alert(response, false)
                    } else {
                        modal_alert(response, false);
                        // if (response.result.status === "W") {
                        //     modal_alert({message : translation.class_full_you_in_waiting_list}, false)
                        // }
                        if (response.result.show_success_finish_freetrial_tutorial) {
                            $(".popover").popover('destroy'); // destroy freetrial tutorial popover
                            $("#modal_dialog").html(response.result.html);
                            $("#modal_dialog").modal('show');
                            window.setTimeout(init_hide_freetrial_tutorial, 200);
                            init_hide_freetrial_tutorial();
                        }
                        my_schedule();
                        calendar_sync();
                    }
                }
            });
        };

        if (left < count) {
            $('#confirm').attr('disabled', false);
            confirm_function_2({ message : translation.user_book_waiting_list }, callback);
        } else callback();
    });
}

// end user functions
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


function init_freetrial_tutorial_pagination() {
    $("#freetriat-tutorial-1").click(function() {
        set_freetrial_tutorial_step(1);
        setTimeout(function () {
            location.reload();
        }, 500);
        // class_room = typeof class_room !== 'undefined' ? class_room : $('#class_room').val();
        // var default_mode = $('#default_mode').val();
        // var default_date = $('#default_date').val();
        // move_to_period(default_date, default_mode, class_room);
    });
    $("#freetriat-tutorial-2").click(function() {
        set_freetrial_tutorial_step(2);
        setTimeout(function () {
            location.reload();
        }, 500);
        // class_room = typeof class_room !== 'undefined' ? class_room : $('#class_room').val();
        // var default_mode = $('#default_mode').val();
        // var default_date = $('#default_date').val();
        // move_to_period(default_date, default_mode, class_room);
    });
    $("#freetriat-tutorial-3").click(function() {
        set_freetrial_tutorial_step(3);
        setTimeout(function () {
            location.reload();
        }, 500);
        // class_room = typeof class_room !== 'undefined' ? class_room : $('#class_room').val();
        // var default_mode = $('#default_mode').val();
        // var default_date = $('#default_date').val();
        // move_to_period(default_date, default_mode, class_room);
    });
}

function init_hide_freetrial_tutorial() {
    $("#finish").click(function () {
        $("div.table-calender table td:has(div.table-chk):first div").popover('destroy');
        $.ajax({
            type: 'POST',
            url: 'ajax/userdata/hide_freetial_tutorial',
            data: {
                hide: 1
            },
            success: function (data) {
                $("#modal_dialog").modal('hide');
            }
        });
    });
}

function show_popup_freetrial1(options) {
    if (options.result.tutorial_step < 3) {
        var popover = $("[name='class_room']:first");
        var room = $("[name='class_room']");
        if (popover.length !== 0 && room.length > 1) {
            set_freetrial_tutorial_step(2);
            popover.popover({
                content : options.result.select_room_html,
                html: true,
                placement: 'top',
                trigger: 'manual'
            });
            popover.popover('show');
            if (popover.length !== 0) {
                $('html, body').animate({
                    scrollTop: popover.offset().top - 200
                }, 1000);
            }
            init_freetrial_tutorial_pagination();
            $("div.popover").addClass('background_blue_color freetrial-popover-experience');
            $("#next_popup").click(function () {
                $("#modal_dialog").modal('hide');
                popover.popover('destroy');
                $(".popovers").popover('destroy');
                window.setTimeout(function() {
                    show_popup_freetrial2(options)
                }, 200);
            });
        } else {
            set_freetrial_tutorial_step(3);
            window.setTimeout(function() {
                show_popup_freetrial2(options)
            }, 200);
        }
    } else {
        show_popup_freetrial2(options);
    }
}

function set_freetrial_tutorial_step(tutorial_step) {
    $.ajax({
        type: 'POST',
        url: 'ajax/userdata/set_freetrial_tutorial_step',
        data: {
            tutorial_step: tutorial_step
        },
        success: function (data) {

        }
    });
}

function show_popup_freetrial2(options) {
    set_freetrial_tutorial_step(3);
    var popover = $("div.table-calender table td:has(div.table-chk):first div");
    popover.popover({
        content : options.result.select_class_html,
        html: true,
        placement: 'top',
        trigger: 'manual'
    });

    if (popover.length !== 0) {
        popover.popover('show');
        $('html, body').animate({
            scrollTop: popover.offset().top - 200
        }, 1000);
        $("div.popover").addClass('background_blue_color freetrial-popover-experience select-class');
        $("#finish").click(function () {
            $("div.table-calender table td:has(div.table-chk):first div").popover('destroy');
        });
    } else {
        $("#modal_dialog").html(options.result.no_classes_html);
        window.setTimeout(function() {
            $("#modal_dialog").modal('show');
        }, 200);

        $("#finish").click(function () {
            $("#modal_dialog").modal('hide');
        });
    }
    window.setTimeout(init_freetrial_tutorial_pagination, 200);

}
